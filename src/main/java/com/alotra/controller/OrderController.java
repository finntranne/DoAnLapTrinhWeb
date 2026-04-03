package com.alotra.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.location.Address;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderHistory;
import com.alotra.entity.order.OrderItem;
import com.alotra.entity.order.Payment;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.enums.PaymentMethod;
import com.alotra.enums.PaymentStatus;
import com.alotra.repository.cart.CartItemRepository;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.location.AddressRepository;
import com.alotra.repository.order.OrderHistoryRepository;
import com.alotra.repository.order.OrderItemRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.PaymentRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.checkout.VNPayService;
import com.alotra.service.notification.NotificationService;
import com.alotra.service.order.ShipperOrderService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.shop.StoreService;
import com.alotra.service.user.UserService;
import com.alotra.util.OrderPricingUtils;
import com.alotra.util.PdfGeneratorService;
import com.alotra.view.address.AddressDisplayView;
import com.alotra.view.cart.CheckoutCartItemView;
import com.alotra.view.order.OrderView;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private static final String VIETQR_BANK_ID = "970405";
    private static final String VIETQR_ACCOUNT_NO = "4303205336551";
    private static final String VIETQR_API_URL = "https://img.vietqr.io/image/";
    private static final String VIETQR_TEMPLATE = "compact";
    private static final String VIETQR_ADD_INFO = "Thanh toan don hang #";
    private static final String VIETQR_ACCOUNT_NAME = "TRAN HUU THOAI";
    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("20000");

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private UserService userService;
    @Autowired
    private VNPayService vnPayService;
    @Autowired
    private StoreService storeService;
    @Autowired
    private PromotionRepository promotionRepository;
    @Autowired
    private OrderHistoryRepository orderHistoryRepository;
    @Autowired
    private ShipperOrderService shipperOrderService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PdfGeneratorService pdfGeneratorService;
    @Autowired
    private NotificationService notificationService;

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap.");
        }

        String username = auth.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung: " + username));
    }

    private int getCurrentCartItemCount() {
        try {
            return cartService.getCartItemCount(getCurrentAuthenticatedUser());
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return 0;
        }
    }

    private Integer getSelectedShopId(HttpSession session) {
        Integer selectedShopId = (Integer) session.getAttribute("selectedShopId");
        return selectedShopId == null ? 0 : selectedShopId;
    }

    private User assignShipperAutomatically(Integer shopId) {
        List<User> shippers = userRepository.findByRoles_RoleName("SHIPPER").stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .toList();

        User selected = null;
        long minActiveOrders = Long.MAX_VALUE;

        for (User shipper : shippers) {
            long activeOrderCount = orderRepository.countByShipper_IdAndOrderStatus(shipper.getId(), "Delivering")
                    + orderRepository.countByShipper_IdAndOrderStatus(shipper.getId(), "Confirmed");
            if (activeOrderCount < minActiveOrders) {
                minActiveOrders = activeOrderCount;
                selected = shipper;
            }
        }

        return selected;
    }

    @PostMapping("/cart/select-for-checkout")
    public String selectItemsForCheckout(
            @RequestParam(name = "selectedItemIds", required = false) List<Integer> selectedItemIds,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui long chon it nhat mot san pham de thanh toan.");
            return "redirect:/cart";
        }

        try {
            User user = getCurrentAuthenticatedUser();
            Cart cart = cartRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Khong tim thay gio hang"));

            Set<Integer> userCartItemIds = cart.getItems().stream()
                    .map(CartItem::getCartItemID)
                    .collect(Collectors.toSet());

            List<Integer> validSelectedIds = selectedItemIds.stream()
                    .filter(userCartItemIds::contains)
                    .toList();

            if (validSelectedIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "San pham da chon khong hop le hoac khong con trong gio.");
                return "redirect:/cart";
            }

            session.setAttribute("selectedCheckoutItemIds", validSelectedIds);
            session.removeAttribute("currentCouponCode");
            session.removeAttribute("currentDiscountAmount");
            return "redirect:/checkout";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (Exception ex) {
            log.error("Loi chon item checkout", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Da xay ra loi khi chon san pham.");
            return "redirect:/cart";
        }
    }

    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Integer selectedShopId = getSelectedShopId(session);

            @SuppressWarnings("unchecked")
            List<Integer> selectedItemIds = (List<Integer>) session.getAttribute("selectedCheckoutItemIds");

            if (selectedItemIds == null || selectedItemIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui long chon san pham tu gio hang truoc.");
                return "redirect:/cart";
            }

            User user = getCurrentAuthenticatedUser();
            Cart cart = cartRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Khong tim thay gio hang"));

            List<CartItem> itemsToCheckout = cart.getItems().stream()
                    .filter(item -> selectedItemIds.contains(item.getCartItemID()))
                    .toList();

            if (itemsToCheckout.isEmpty()) {
                session.removeAttribute("selectedCheckoutItemIds");
                redirectAttributes.addFlashAttribute("errorMessage", "San pham da chon khong con trong gio.");
                return "redirect:/cart";
            }

            List<Map<String, Object>> checkoutItemVMs = new ArrayList<>();
            for (CartItem item : itemsToCheckout) {
                Map<String, Object> vm = new HashMap<>();
                vm.put("item", new CheckoutCartItemView(item));
                vm.put("lineTotal", cartService.getLineTotal(item));
                checkoutItemVMs.add(vm);
            }

            BigDecimal subtotal = cartService.calculateSubtotal(itemsToCheckout.stream().collect(Collectors.toSet()));
            BigDecimal shippingFee = DEFAULT_SHIPPING_FEE;
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal grandTotal = subtotal.add(shippingFee);

            List<AddressDisplayView> addresses = addressRepository.findByUserId(user.getId()).stream()
                    .map(address -> new AddressDisplayView(address, user))
                    .toList();

            model.addAttribute("checkoutItemVMs", checkoutItemVMs);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("discount", discount);
            model.addAttribute("grandTotal", grandTotal);
            model.addAttribute("addresses", addresses);
            model.addAttribute("user", user);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

            return "shop/checkout";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (Exception ex) {
            log.error("Loi tai trang checkout", ex);
            session.removeAttribute("selectedCheckoutItemIds");
            redirectAttributes.addFlashAttribute("errorMessage", "Loi tai trang thanh toan.");
            return "redirect:/cart";
        }
    }

    @PostMapping("/apply-coupon")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyCoupon(@RequestParam("couponCode") String couponCode,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        response.put("error",
                "Entity don hang hien tai khong luu giam gia cap don. Ma giam gia don hang da duoc tat.");
        session.removeAttribute("currentCouponCode");
        session.removeAttribute("currentDiscountAmount");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/remove-coupon")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeCoupon(HttpSession session) {
        session.removeAttribute("currentCouponCode");
        session.removeAttribute("currentDiscountAmount");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/place-order")
    @Transactional
    public Object placeOrder(@RequestParam(required = false) Integer addressId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(name = "selectedItemIds") List<Integer> selectedItemIds,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (addressId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui long chon dia chi giao hang.");
            return "redirect:/checkout";
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui long chon phuong thuc thanh toan.");
            return "redirect:/checkout";
        }

        try {
            if (selectedItemIds == null || selectedItemIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Khong co san pham nao duoc chon de dat hang.");
                return "redirect:/cart";
            }

            User user = getCurrentAuthenticatedUser();
            Cart cart = cartRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Khong tim thay gio hang"));

            List<CartItem> itemsToOrder = cart.getItems().stream()
                    .filter(item -> selectedItemIds.contains(item.getCartItemID()))
                    .toList();

            if (itemsToOrder.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Khong co san pham hop le trong gio hang.");
                return "redirect:/cart";
            }

            Shop shop = itemsToOrder.get(0).getVariant().getProduct().getShop();
            boolean mixedShop = itemsToOrder.stream()
                    .map(item -> item.getVariant().getProduct().getShop().getShopId())
                    .anyMatch(shopId -> !shopId.equals(shop.getShopId()));
            if (mixedShop) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Don hang hien tai chi ho tro thanh toan cac san pham cung mot cua hang.");
                return "redirect:/checkout";
            }

            Address chosenAddress = addressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Dia chi giao hang khong hop le."));

            PaymentMethod paymentMethodEnum = resolvePaymentMethod(paymentMethod);
            String initialStatus = paymentMethodEnum == PaymentMethod.COD ? "Confirmed" : "Pending";
            User assignedShipper = paymentMethodEnum == PaymentMethod.COD
                    ? assignShipperAutomatically(shop.getShopId())
                    : null;

            Order order = new Order();
            order.setUser(user);
            order.setShop(shop);
            order.setAddress(chosenAddress);
            order.setOrderDate(LocalDateTime.now());
            order.setOrderStatus(initialStatus);
            order.setShippingFee(DEFAULT_SHIPPING_FEE);
            order.setNotes(notes);
            order.setShipper(assignedShipper);
            order = orderRepository.save(order);

            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem cartItem : itemsToOrder) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setVariant(cartItem.getVariant());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItems.add(orderItem);
            }
            orderItemRepository.saveAll(orderItems);
            order.setItems(orderItems);

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setMethod(paymentMethodEnum);
            payment.setStatus(PaymentStatus.UNPAID);
            paymentRepository.save(payment);

            OrderHistory history = new OrderHistory();
            history.setOrder(order);
            history.setOldStatus(null);
            history.setNewStatus(initialStatus);
            history.setChangedByUser(user);
            history.setTimestamp(LocalDateTime.now());
            history.setNotes("Don hang duoc tao tu he thong checkout");
            orderHistoryRepository.save(history);

            if (shop.getUser() != null) {
                try {
                    notificationService.notifyVendorAboutNewOrder(shop.getUser().getId(), order.getOrderID(),
                            user.getFullName());
                } catch (Exception ex) {
                    log.warn("Khong gui duoc thong bao cho vendor: {}", ex.getMessage());
                }
            }

            if (assignedShipper != null) {
                try {
                    shipperOrderService.createInitialShippingHistory(order.getOrderID(), assignedShipper.getId(),
                            "Don hang duoc gan tu dong cho shipper");
                    notificationService.notifyShipperAboutAssignment(assignedShipper.getId(), order.getOrderID(),
                            OrderPricingUtils.formatAddress(chosenAddress));
                } catch (Exception ex) {
                    log.warn("Khong gui duoc thong bao shipper: {}", ex.getMessage());
                }
            }

            for (CartItem item : itemsToOrder) {
                item.getToppings().clear();
                cart.removeItem(item);
                cartItemRepository.delete(item);
            }
            cartRepository.save(cart);

            session.removeAttribute("selectedCheckoutItemIds");
            session.removeAttribute("currentCouponCode");
            session.removeAttribute("currentDiscountAmount");

            if (paymentMethodEnum == PaymentMethod.VNPAY) {
                String paymentUrl = vnPayService.createPaymentUrl(order, request);
                return new RedirectView(paymentUrl);
            }

            if (paymentMethodEnum == PaymentMethod.BANK_TRANSFER) {
                BigDecimal amount = OrderPricingUtils.calculateOrderTotal(order);
                String description = VIETQR_ADD_INFO + order.getOrderID();
                redirectAttributes.addFlashAttribute("orderId", order.getOrderID());
                redirectAttributes.addFlashAttribute("qrUrl", buildVietQrUrl(order.getOrderID(), amount, description));
                redirectAttributes.addFlashAttribute("amount", amount);
                redirectAttributes.addFlashAttribute("description", description);
                return "redirect:/vietqr-confirmation";
            }

            redirectAttributes.addFlashAttribute("orderId", order.getOrderID());
            return "redirect:/order-success";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            session.removeAttribute("selectedCheckoutItemIds");
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loi dat hang: " + ex.getMessage());
            return "redirect:/checkout";
        } catch (Exception ex) {
            log.error("Da xay ra loi he thong khi dat hang", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Da xay ra loi he thong khi dat hang.");
            return "redirect:/checkout";
        }
    }

    @GetMapping("/order-success")
    public String orderSuccessPage(Model model,
            @RequestParam(name = "orderId", required = false) Integer requestOrderId,
            @ModelAttribute("orderId") Integer flashOrderId,
            HttpSession session) {

        Integer selectedShopId = getSelectedShopId(session);
        Integer resolvedOrderId = requestOrderId != null ? requestOrderId : flashOrderId;

        model.addAttribute("orderId", resolvedOrderId);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

        return "shop/order-success";
    }

    @GetMapping("/order-failed")
    public String orderFailedPage(Model model, @RequestParam(required = false) String reason, HttpSession session) {
        Integer selectedShopId = getSelectedShopId(session);

        model.addAttribute("reason", reason);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

        return "shop/order-failed";
    }

    @GetMapping("/vietqr-confirmation")
    public String vietQrConfirmationPage(Model model,
            @ModelAttribute("orderId") Integer orderId,
            @ModelAttribute("qrUrl") String qrUrl,
            @ModelAttribute("amount") BigDecimal amount,
            @ModelAttribute("description") String description) {

        if (orderId == null || qrUrl == null) {
            return "redirect:/order-failed?reason=qr_missing_data";
        }

        model.addAttribute("orderId", orderId);
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("amount", amount);
        model.addAttribute("description", description);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("categories", categoryService.findAll());

        return "shop/vietqr-confirmation";
    }

    @Transactional(readOnly = true)
    @GetMapping("/order/invoice/{orderId}")
    public String showInvoicePage(@PathVariable Integer orderId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            User user = getCurrentAuthenticatedUser();
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang #" + orderId));

            if (!order.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Ban khong co quyen truy cap hoa don nay.");
            }

            Payment payment = paymentRepository.findByOrder_OrderID(orderId).orElse(null);
            OrderView orderView = OrderView.from(order, payment);

            Integer selectedShopId = getSelectedShopId(session);
            model.addAttribute("order", orderView);
            model.addAttribute("orderDetails", orderView.getOrderDetails());
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

            return "shop/invoice";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Khong tim thay hoac khong co quyen truy cap hoa don.");
            return "redirect:/user/orders";
        } catch (Exception ex) {
            log.error("Loi khi tai hoa don #{}", orderId, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Loi he thong khi tai hoa don.");
            return "redirect:/user/orders";
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/order/export-pdf/{orderId}")
    public ResponseEntity<byte[]> exportInvoicePdf(@PathVariable Integer orderId) {
        try {
            User user = getCurrentAuthenticatedUser();
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang #" + orderId));

            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Payment payment = paymentRepository.findByOrder_OrderID(orderId).orElse(null);
            OrderView orderView = OrderView.from(order, payment);

            Map<String, Object> data = new HashMap<>();
            data.put("order", orderView);
            data.put("orderDetails", orderView.getOrderDetails());

            byte[] pdfBytes = pdfGeneratorService.generatePdf("shop/invoice", data);
            String filename = "HoaDon_AloTra_" + orderId + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception ex) {
            log.error("Loi khi tao PDF hoa don #{}", orderId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private PaymentMethod resolvePaymentMethod(String paymentMethod) {
        String normalized = paymentMethod.trim().toUpperCase();
        return switch (normalized) {
            case "CASH", "COD" -> PaymentMethod.COD;
            case "VNPAY" -> PaymentMethod.VNPAY;
            case "VIETQR", "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER;
            case "MOMO" -> PaymentMethod.MOMO;
            case "ZALOPAY" -> PaymentMethod.ZALOPAY;
            default -> throw new IllegalArgumentException("Phuong thuc thanh toan khong hop le.");
        };
    }

    private String buildVietQrUrl(Integer orderId, BigDecimal amount, String description) {
        String roundedAmount = amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        return String.format(
                "%s%s-%s-%s.jpg?amount=%s&addInfo=%s&accountName=%s",
                VIETQR_API_URL,
                VIETQR_BANK_ID,
                VIETQR_ACCOUNT_NO,
                VIETQR_TEMPLATE,
                roundedAmount,
                URLEncoder.encode(description, StandardCharsets.UTF_8),
                URLEncoder.encode(VIETQR_ACCOUNT_NAME, StandardCharsets.UTF_8));
    }
}
