package com.alotra.controller; // Giữ package này

// Import các entity
import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.location.Address;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderDetail;
import com.alotra.entity.order.OrderDetailTopping;
import com.alotra.entity.order.OrderDetailToppingId;
import com.alotra.entity.user.User;
import com.alotra.repository.cart.CartItemRepository;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.order.OrderDetailRepository;
import com.alotra.repository.order.OrderDetailToppingRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.location.AddressRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.checkout.VNPayService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.user.UserService;

// *** THÊM IMPORT CHO LOGIC GIẢM GIÁ ***
import com.alotra.service.product.ProductService;
import com.alotra.model.ProductSaleDTO;
import java.math.RoundingMode;
// **************************************

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; 
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView; 

import java.math.BigDecimal;
import java.time.LocalDateTime; 
import java.util.ArrayList; 
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class OrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private VNPayService vnPayService;
    @Autowired private CartService cartService;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository; 
    @Autowired private AddressRepository addressRepository; 
    @Autowired private CategoryService categoryService;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private OrderDetailToppingRepository orderDetailToppingRepository;
    @Autowired private UserService userService;
    
    // *** THÊM: Inject ProductService để lấy giảm giá ***
    @Autowired 
    private ProductService productService;

    // === HÀM TRỢ GIÚP: Lấy User (Giữ nguyên) ===
    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        String username = auth.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + username));
    }

    // --- Hàm trợ giúp lấy số lượng giỏ hàng (Giữ nguyên) ---
    private int getCurrentCartItemCount() {
        try {
            User user = getCurrentAuthenticatedUser();
            return cartService.getCartItemCount(user);
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return 0;
        }
    }

    // --- Hàm CHỌN ITEM CHECKOUT (Giữ nguyên) ---
    @PostMapping("/cart/select-for-checkout")
    public String selectItemsForCheckout(@RequestParam(name = "selectedItemIds", required = false) List<Integer> selectedItemIds,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {

        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một sản phẩm để thanh toán.");
            return "redirect:/cart";
        }

        try {
            User user = getCurrentAuthenticatedUser(); 
            Cart cart = cartRepository.findByUser_Id(user.getId()) 
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

            Set<Integer> userCartItemIds = cart.getItems().stream()
                    .map(CartItem::getCartItemID) 
                    .collect(Collectors.toSet());

            List<Integer> validSelectedIds = selectedItemIds.stream()
                    .filter(userCartItemIds::contains)
                    .collect(Collectors.toList());

            if (validSelectedIds.isEmpty()) {
                 redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm đã chọn không hợp lệ hoặc không còn trong giỏ.");
                 return "redirect:/cart";
            }
            
            session.setAttribute("selectedCheckoutItemIds", validSelectedIds);
            return "redirect:/checkout"; 

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
             System.err.println("Lỗi chọn item checkout: " + e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi chọn sản phẩm.");
             return "redirect:/cart";
        }
    }


    /**
     * *** HIỂN THỊ TRANG CHECKOUT (Giữ nguyên logic sau sửa chữa) ***
     */
    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) { 

        try {
            @SuppressWarnings("unchecked")
            List<Integer> selectedItemIds = (List<Integer>) session.getAttribute("selectedCheckoutItemIds");

            if (selectedItemIds == null || selectedItemIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn sản phẩm từ giỏ hàng trước.");
                return "redirect:/cart"; 
            }

            User user = getCurrentAuthenticatedUser(); 
            Cart cart = cartRepository.findByUser_Id(user.getId()) 
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

            // Lọc items
            List<CartItem> itemsToCheckout = cart.getItems().stream()
                    .filter(item -> selectedItemIds.contains(item.getCartItemID())) 
                    .collect(Collectors.toList());

            if (itemsToCheckout.isEmpty()) {
                 session.removeAttribute("selectedCheckoutItemIds");
                 redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm đã chọn không còn trong giỏ.");
                 return "redirect:/cart";
            }

            // Tạo ViewModel
            List<Map<String, Object>> checkoutItemVMs = new ArrayList<>();
            for (CartItem item : itemsToCheckout) {
                Map<String, Object> vm = new HashMap<>();
                vm.put("item", item); 
                vm.put("lineTotal", cartService.getLineTotal(item)); 
                checkoutItemVMs.add(vm);
            }

            // Tính toán tổng tiền
            BigDecimal subtotal = cartService.calculateSubtotal(new HashSet<>(itemsToCheckout));
            
            BigDecimal shippingFee = new BigDecimal("20000"); 
            BigDecimal discount = BigDecimal.ZERO; 
            BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);

            List<Address> addresses = addressRepository.findByUserId(user.getId());

            // Đưa thông tin ra View
            model.addAttribute("checkoutItemVMs", checkoutItemVMs); 
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("discount", discount); 
            model.addAttribute("grandTotal", grandTotal);
            model.addAttribute("addresses", addresses);
            model.addAttribute("user", user); 

            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());

            return "shop/checkout";

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Lỗi khi tải trang checkout: " + e.getMessage());
            e.printStackTrace();
            session.removeAttribute("selectedCheckoutItemIds"); 
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tải trang thanh toán.");
            return "redirect:/cart";
        }
    }

    /**
     * *** SỬA LỚN: XỬ LÝ ĐẶT HÀNG (ĐẢM BẢO SUBTOTAL VÀ ID TRƯỚC KHI LƯU) ***
     */
    @PostMapping("/place-order")
    @Transactional
    public Object placeOrder(
            @RequestParam(required = false) Integer addressId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String paymentMethod,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (addressId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn địa chỉ giao hàng.");
            return "redirect:/checkout";
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn phương thức thanh toán.");
            return "redirect:/checkout";
        }

        Order savedOrder = null;

        try {
            // 1-5. Lấy data và tính tổng (Giữ nguyên)
            @SuppressWarnings("unchecked")
            List<Integer> selectedItemIds = (List<Integer>) session.getAttribute("selectedCheckoutItemIds");

            if (selectedItemIds == null || selectedItemIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn hoặc chưa chọn sản phẩm.");
                return "redirect:/cart";
            }

            User user = getCurrentAuthenticatedUser();
            Cart cart = cartRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

            Map<Integer, CartItem> cartItemMap = cart.getItems().stream()
                    .filter(item -> selectedItemIds.contains(item.getCartItemID()))
                    .collect(Collectors.toMap(CartItem::getCartItemID, item -> item));

            List<CartItem> itemsToOrder = new ArrayList<>(cartItemMap.values());

            if (itemsToOrder.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không có sản phẩm hợp lệ trong giỏ hàng.");
                return "redirect:/cart";
            }

            Address chosenAddress = addressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Địa chỉ giao hàng không hợp lệ."));

            if (!chosenAddress.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Bạn không có quyền dùng địa chỉ này.");
            }

            BigDecimal subtotal = cartService.calculateSubtotal(new HashSet<>(itemsToOrder));
            BigDecimal shippingFee = new BigDecimal("20000");
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);

            // 6. Tạo và lưu Order (Giữ nguyên)
            Order order = new Order();
            order.setUser(user);
            order.setShop(itemsToOrder.get(0).getVariant().getProduct().getShop());
            order.setPaymentMethod(paymentMethod);
            order.setShippingAddress(chosenAddress.getFullAddress());
            order.setRecipientName(chosenAddress.getRecipientName());
            order.setRecipientPhone(chosenAddress.getPhoneNumber());
            order.setSubtotal(subtotal);
            order.setShippingFee(shippingFee);
            order.setDiscountAmount(discount);
            order.setGrandTotal(grandTotal);
            order.setNotes(notes);

            savedOrder = orderRepository.saveAndFlush(order);

            // === 7. SỬA LỚN: Xử lý OrderDetail và Topping ===
            List<OrderDetail> orderDetailList = new ArrayList<>();
            List<OrderDetailTopping> orderDetailToppingList = new ArrayList<>(); 

            for (CartItem cartItem : itemsToOrder) {
                
                // --- 7a. TÍNH TOÁN GIÁ ĐƠN VỊ ĐÃ GIẢM ---
                BigDecimal baseVariantPrice = cartItem.getVariant().getPrice();
                Integer discountPercent = null;
                if (cartItem.getVariant().getProduct() != null && cartItem.getVariant().getProduct().getProductID() != null) {
                    Optional<ProductSaleDTO> saleDTOOpt = productService.findProductSaleDataById(
                        cartItem.getVariant().getProduct().getProductID()
                    );
                    if (saleDTOOpt.isPresent()) {
                        discountPercent = saleDTOOpt.get().getDiscountPercentage();
                    }
                }
                
                BigDecimal discountedVariantPrice; 
                if (discountPercent != null && discountPercent > 0) {
                    BigDecimal multiplier = new BigDecimal(100 - discountPercent).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                    discountedVariantPrice = baseVariantPrice.multiply(multiplier);
                } else {
                    discountedVariantPrice = baseVariantPrice;
                }
                // --- KẾT THÚC TÍNH GIÁ ĐƠN VỊ ĐÃ GIẢM ---

                // --- 7b. TÍNH VÀ SET SUBTOTAL HOÀN CHỈNH ---
                OrderDetail detail = new OrderDetail();
                detail.setOrder(savedOrder);
                detail.setVariant(cartItem.getVariant());
                detail.setQuantity(cartItem.getQuantity());
                detail.setUnitPrice(discountedVariantPrice); 

                BigDecimal itemPricePerUnit = discountedVariantPrice;
                
                // Tính toán giá topping để cộng vào đơn vị
                if (cartItem.getSelectedToppings() != null) {
                    for (var topping : cartItem.getSelectedToppings()) {
                        itemPricePerUnit = itemPricePerUnit.add(topping.getAdditionalPrice());
                    }
                }
                
                // Tính Final Subtotal = (Giá đơn vị + Topping) * Số lượng
                BigDecimal finalSubtotal = itemPricePerUnit.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

                // *** THAY ĐỔI LỚN: SET SUBTOTAL TRƯỚC KHI LƯU LẦN 1 ***
                detail.setSubtotal(finalSubtotal); 
                
                // --- 7c. LƯU OrderDetail (Chỉ 1 lần để lấy ID) ---
                OrderDetail savedDetail = orderDetailRepository.saveAndFlush(detail); 
                
                // --- 7d. TẠO OrderDetailTopping (Sau khi OrderDetail có ID) ---
                if (cartItem.getSelectedToppings() != null) {
                    for (var topping : cartItem.getSelectedToppings()) {
                        OrderDetailTopping odt = new OrderDetailTopping();
                        odt.setOrderDetail(savedDetail); 
                        odt.setTopping(topping);
                        odt.setUnitPrice(topping.getAdditionalPrice());
                        
                        // Thiết lập khóa phức hợp thủ công
                        OrderDetailToppingId id = new OrderDetailToppingId();
                        id.setOrderDetailID(savedDetail.getOrderDetailID()); 
                        id.setToppingID(topping.getToppingID());
                        odt.setId(id); 

                        orderDetailToppingList.add(odt);
                    }
                }
                
                orderDetailList.add(savedDetail);
            }

            // 7e. LƯU TẤT CẢ TOPPING
            if (!orderDetailToppingList.isEmpty()) {
                orderDetailToppingRepository.saveAll(orderDetailToppingList);
            }

            // === 8. Xóa cart items (Giữ nguyên) ===
            for (CartItem item : itemsToOrder) {
                item.getSelectedToppings().clear();
                cart.removeItem(item);
                cartItemRepository.delete(item);
            }
            cartItemRepository.flush();
            cartRepository.save(cart);

            // === 9. Xóa session (Giữ nguyên) ===
            session.removeAttribute("selectedCheckoutItemIds");

            // === 10. Thanh toán (Giữ nguyên) ===
            if ("Cash".equalsIgnoreCase(paymentMethod)) {
                savedOrder.setOrderStatus("Pending");
                savedOrder.setPaymentStatus("Unpaid");
                orderRepository.save(savedOrder);
                redirectAttributes.addFlashAttribute("orderId", savedOrder.getOrderID());
                return "redirect:/order-success";
            }

            if ("VNPay".equalsIgnoreCase(paymentMethod)) {
                try {
                    String paymentUrl = vnPayService.createPaymentUrl(savedOrder, request);
                    
                    // *** THAY ĐỔI QUAN TRỌNG: CẬP NHẬT TRẠNG THÁI TRƯỚC KHI REDIRECT ***
                    savedOrder.setOrderStatus("Pending Payment"); 
                    savedOrder.setPaymentStatus("Unpaid"); // Vẫn chưa thanh toán
                    orderRepository.save(savedOrder); 
                    
                    return new RedirectView(paymentUrl);
                } catch (Exception e) {
                    // Nếu lỗi khi tạo URL, chuyển hướng về checkout với thông báo lỗi
                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo liên kết thanh toán VNPay: " + e.getMessage());
                    return "redirect:/checkout";
                }
            }

            redirectAttributes.addFlashAttribute("errorMessage", "Phương thức thanh toán không hợp lệ.");
            return "redirect:/checkout";

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            if (session != null) session.removeAttribute("selectedCheckoutItemIds");
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đặt hàng: " + e.getMessage());
            return "redirect:/checkout";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi hệ thống khi đặt hàng.");
            return "redirect:/checkout";
        }
    }


    // Trang thành công (Giữ nguyên)
    @GetMapping("/order-success")
    public String orderSuccessPage(Model model, @ModelAttribute("orderId") Integer orderId) {
        model.addAttribute("orderId", orderId);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("categories", categoryService.findAll());
        return "shop/order-success"; 
    }

    // Trang thất bại (Giữ nguyên)
    @GetMapping("/order-failed")
    public String orderFailedPage(Model model, @RequestParam(required = false) String reason) {
        model.addAttribute("reason", reason);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("categories", categoryService.findAll());
        return "shop/order-failed"; 
    }
    
 // =======================================================
    // *** HÀM MỚI: XỬ LÝ KẾT QUẢ TRẢ VỀ TỪ VNPAY ***
    // =======================================================
    @GetMapping("/vnpay_return")
    @Transactional
    public String vnpayReturn(HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
        
        // 1. Kiểm tra chữ ký (Checksum) và kết quả giao dịch
        String status = vnPayService.paymentReturn(request); // Giả sử service này trả về "00" nếu Checksum hợp lệ

        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_TxnRef = request.getParameter("vnp_TxnRef"); // OrderID (Mã đơn hàng)
        
        Integer orderId = null;
        try {
            if (vnp_TxnRef != null && !vnp_TxnRef.isEmpty()) {
                orderId = Integer.parseInt(vnp_TxnRef);
            }
        } catch (NumberFormatException e) {
            // Không tìm thấy OrderID hoặc lỗi parse
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Không tìm thấy mã đơn hàng trong phản hồi VNPay.");
            return "redirect:/order-failed";
        }

        Order order = orderRepository.findById(orderId)
                .orElse(null);

        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Đơn hàng không tồn tại.");
            return "redirect:/order-failed";
        }

        // 2. Xử lý kết quả dựa trên Response Code và Checksum
        if ("00".equals(status) && "00".equals(vnp_ResponseCode)) {
            // Thanh toán thành công và Checksum hợp lệ
            
            // Cập nhật trạng thái đơn hàng và thanh toán
            if (!"Paid".equals(order.getPaymentStatus())) { // Chỉ cập nhật nếu chưa Paid (để tránh xử lý lại)
                 order.setPaymentStatus("Paid");
                 order.setOrderStatus("Processing"); // Đã thanh toán, chuyển sang xử lý
                 orderRepository.save(order);
            }
            
            redirectAttributes.addFlashAttribute("orderId", orderId);
            return "redirect:/order-success";

        } else if ("00".equals(status) && !"00".equals(vnp_ResponseCode)) {
            // Checksum hợp lệ nhưng giao dịch thất bại (ví dụ: ngân hàng từ chối)
            
            // Cập nhật trạng thái đơn hàng 
            order.setPaymentStatus("Payment Failed");
            order.setOrderStatus("Cancelled");
            orderRepository.save(order);
            
            String reason = "Giao dịch thất bại. Mã lỗi VNPay: " + vnp_ResponseCode;
            redirectAttributes.addFlashAttribute("reason", reason);
            return "redirect:/order-failed";
        } else {
            // Checksum không hợp lệ (Lỗi bảo mật/server)
            
            order.setPaymentStatus("Unverified");
            order.setOrderStatus("Pending Check"); // Yêu cầu admin kiểm tra
            orderRepository.save(order);
            
            redirectAttributes.addFlashAttribute("reason", "Lỗi bảo mật/kết nối (Checksum không hợp lệ). Vui lòng kiểm tra lại đơn hàng.");
            return "redirect:/order-failed";
        }
    }
}