package com.alotra.controller; // Giữ package này

// Import các entity đã merge
import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.location.Address;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderDetail; // *** SỬA: Dùng OrderDetail ***
import com.alotra.entity.order.OrderDetailTopping; // *** SỬA: Dùng OrderDetailTopping ***
import com.alotra.entity.order.OrderDetailToppingId;
import com.alotra.entity.user.User; // Sử dụng User
import com.alotra.repository.cart.CartItemRepository;
// Import Service và Repository đã merge
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.order.OrderDetailRepository; // *** THÊM: Repository cho OrderDetail ***
import com.alotra.repository.order.OrderDetailToppingRepository; // *** THÊM: Repository cho OrderDetailTopping ***
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.location.AddressRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.checkout.VNPayService;
import com.alotra.service.product.CategoryService;
// import com.alotra.service.user.CustomerService; // *** BỎ CustomerService ***
import com.alotra.service.user.UserService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional; // Import Transactional (cần thiết cho placeOrder)

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Qualifier; // Bỏ nếu không cần
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Thêm exception
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; // Dùng *
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView; // Import RedirectView

import java.math.BigDecimal;
import java.time.LocalDateTime; // Dùng LocalDateTime
import java.util.ArrayList; // Import ArrayList
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
// Có thể đổi RequestMapping nếu muốn, ví dụ "/checkout"
public class OrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private VNPayService vnPayService;
    @Autowired private CartService cartService;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository; // *** THÊM: Cần để xóa CartItem ***
    @Autowired private AddressRepository addressRepository; // Đã sửa tên biến
    @Autowired private CategoryService categoryService;
    // *** THÊM: Repository cho OrderDetail & Topping ***
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private OrderDetailToppingRepository orderDetailToppingRepository;

    // @Autowired private CustomerService customerService; // *** BỎ ***
    @Autowired private UserService userService; // *** GIỮ ***

    // === HÀM TRỢ GIÚP: Lấy User (Giống logic của CartController) ===
    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        String username = auth.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + username));
    }

    // --- Hàm trợ giúp lấy số lượng giỏ hàng (Giống CartController) ---
    private int getCurrentCartItemCount() {
        try {
            User user = getCurrentAuthenticatedUser();
            return cartService.getCartItemCount(user);
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return 0;
        }
    }

    /**
     * CHỌN ITEM TỪ GIỎ HÀNG ĐỂ CHECKOUT (ĐÃ SỬA)
     * Nhận POST từ form trong cart.html
     */
    @PostMapping("/cart/select-for-checkout")
    public String selectItemsForCheckout(@RequestParam(name = "selectedItemIds", required = false) List<Integer> selectedItemIds, // *** SỬA: Dùng Integer ***
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {

        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một sản phẩm để thanh toán.");
            return "redirect:/cart";
        }

        try {
            User user = getCurrentAuthenticatedUser(); // Lấy User
            Cart cart = cartRepository.findByUser_Id(user.getId()) // *** SỬA: findByUser_Id ***
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

            // Lấy TẤT CẢ item ID hợp lệ của user (kiểu Integer)
            Set<Integer> userCartItemIds = cart.getItems().stream()
                    .map(CartItem::getCartItemID) // ID là Integer
                    .collect(Collectors.toSet());

            // Lọc ra danh sách ID hợp lệ mà user đã chọn
            List<Integer> validSelectedIds = selectedItemIds.stream()
                    .filter(userCartItemIds::contains)
                    .collect(Collectors.toList());

            if (validSelectedIds.isEmpty()) {
                 redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm đã chọn không hợp lệ hoặc không còn trong giỏ.");
                 return "redirect:/cart";
            }

            // Lưu danh sách ID (Integer) hợp lệ vào Session
            session.setAttribute("selectedCheckoutItemIds", validSelectedIds);

            return "redirect:/checkout"; // Chuyển đến trang checkout

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
             System.err.println("Lỗi chọn item checkout: " + e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi chọn sản phẩm.");
             return "redirect:/cart";
        }
    }


    /**
     * HIỂN THỊ TRANG CHECKOUT (ĐÃ SỬA)
     */
    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) { // Thêm RedirectAttributes

        try {
            // LẤY ID (Integer) TỪ SESSION
            @SuppressWarnings("unchecked")
            List<Integer> selectedItemIds = (List<Integer>) session.getAttribute("selectedCheckoutItemIds");

            if (selectedItemIds == null || selectedItemIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn sản phẩm từ giỏ hàng trước.");
                return "redirect:/cart"; // Quay về giỏ hàng
            }

            User user = getCurrentAuthenticatedUser(); // Lấy User

            // Lấy giỏ hàng VÀ LỌC items
            Cart cart = cartRepository.findByUser_Id(user.getId()) // *** SỬA: findByUser_Id ***
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

            // Lọc danh sách items CHỈ BAO GỒM những cái đã chọn (dùng Integer ID)
            List<CartItem> itemsToCheckout = cart.getItems().stream()
                    .filter(item -> selectedItemIds.contains(item.getCartItemID())) // So sánh Integer ID
                    .collect(Collectors.toList());

            if (itemsToCheckout.isEmpty()) {
                 session.removeAttribute("selectedCheckoutItemIds");
                 redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm đã chọn không còn trong giỏ.");
                 return "redirect:/cart";
            }

            // TÍNH TOÁN LẠI TỔNG TIỀN (dùng calculateSubtotal của Service cho gọn)
            BigDecimal subtotal = cartService.calculateSubtotal(new HashSet<>(itemsToCheckout)); // Chuyển List thành Set để gọi service

            // Giả sử phí ship và giảm giá (cần logic thực tế)
            BigDecimal shippingFee = new BigDecimal("20000"); // Ví dụ
            BigDecimal discount = BigDecimal.ZERO; // Cần logic lấy discount áp dụng
            BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);

            // Lấy danh sách địa chỉ của User
            // *** SỬA: Gọi repository với User ID ***
            List<Address> addresses = addressRepository.findByUserId(user.getId());

            // Đưa thông tin ra View
            model.addAttribute("cartItems", itemsToCheckout); // Danh sách đã lọc
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("discount", discount); // Thêm discount để hiển thị
            model.addAttribute("grandTotal", grandTotal);
            model.addAttribute("addresses", addresses);
            model.addAttribute("user", user); // Truyền User thay vì Customer

            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());

            return "shop/checkout"; // Tên view checkout của bạn

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Lỗi khi tải trang checkout: " + e.getMessage());
            e.printStackTrace();
            session.removeAttribute("selectedCheckoutItemIds"); // Xóa session nếu lỗi
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tải trang thanh toán.");
            return "redirect:/cart";
        }
    }

    /**
     * XỬ LÝ ĐẶT HÀNG (ĐÃ SỬA HOÀN TOÀN)
     */
    @PostMapping("/place-order")
    @Transactional // Quan trọng: Đảm bảo tất cả lưu hoặc không
    public Object placeOrder(
    		@RequestParam(required = false) Integer addressId,
            @RequestParam(required = false) String notes, // Ghi chú
            @RequestParam(required = false) String paymentMethod,
            HttpServletRequest request, // Cần cho VNPay
            HttpSession session,
            RedirectAttributes redirectAttributes) {
    	
    	// <<< Thêm: Kiểm tra addressId ngay từ đầu >>>
        if (addressId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn địa chỉ giao hàng.");
            // Giữ lại session ID nếu có để không mất giỏ hàng
            // session.keepAttributes("selectedCheckoutItemIds"); // Cân nhắc nếu dùng RedirectAttributes
            return "redirect:/checkout"; // Quay lại trang checkout báo lỗi
        }
        
     // <<< Thêm kiểm tra paymentMethod >>>
        if (paymentMethod == null || paymentMethod.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn phương thức thanh toán.");
            return "redirect:/checkout";
        }

        Order savedOrder = null; // Khởi tạo để lưu order đã tạo

        try {
            // 1. LẤY ID ITEMS TỪ SESSION
            @SuppressWarnings("unchecked")
            List<Integer> selectedItemIds = (List<Integer>) session.getAttribute("selectedCheckoutItemIds");
            if (selectedItemIds == null || selectedItemIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn hoặc chưa chọn sản phẩm.");
                return "redirect:/cart";
            }

            // 2. Lấy User và Cart
            User user = getCurrentAuthenticatedUser();
            Cart cart = cartRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

            // 3. Lọc lại CartItem cần đặt (để lấy thông tin chi tiết)
            // Dùng Map để truy cập nhanh CartItem theo ID
            Map<Integer, CartItem> cartItemMap = cart.getItems().stream()
                    .filter(item -> selectedItemIds.contains(item.getCartItemID()))
                    .collect(Collectors.toMap(CartItem::getCartItemID, item -> item));

            // Kiểm tra lại nếu item không còn tồn tại
            if (cartItemMap.size() != selectedItemIds.size()) {
                 session.removeAttribute("selectedCheckoutItemIds");
                 redirectAttributes.addFlashAttribute("errorMessage", "Một số sản phẩm đã chọn không còn trong giỏ.");
                 return "redirect:/cart";
            }
            List<CartItem> itemsToOrder = new ArrayList<>(cartItemMap.values()); // List các CartItem cần xử lý

            // 4. Lấy địa chỉ đã chọn và kiểm tra quyền sở hữu
            Address chosenAddress = addressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Địa chỉ giao hàng không hợp lệ."));
            if (!chosenAddress.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Bạn không có quyền sử dụng địa chỉ này.");
            }

            // 5. TÍNH TOÁN LẠI TỔNG TIỀN (Chỉ cho các item được chọn)
            BigDecimal subtotal = cartService.calculateSubtotal(new HashSet<>(itemsToOrder));
            BigDecimal shippingFee = new BigDecimal("20000"); // Cần logic tính phí ship thực tế
            BigDecimal discount = BigDecimal.ZERO;    // Cần logic áp dụng mã giảm giá
            BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);

            // 6. TẠO ĐỐI TƯỢNG ORDER
            Order order = new Order();
            order.setUser(user);
            // Giả sử ShopID cần được xác định (ví dụ: lấy từ sản phẩm đầu tiên?)
            // Cần logic rõ ràng hơn nếu giỏ hàng có thể chứa sản phẩm từ nhiều shop
            if (!itemsToOrder.isEmpty()) {
                 order.setShop(itemsToOrder.get(0).getVariant().getProduct().getShop());
            } else {
                 throw new IllegalStateException("Không có sản phẩm nào để xác định Shop.");
            }
            // order.setPromotion(appliedPromotion); // Cần logic lấy promotion
            // orderDate, orderStatus, paymentStatus được @PrePersist xử lý
            order.setPaymentMethod(paymentMethod);
            order.setShippingAddress(chosenAddress.getFullAddress());
            order.setRecipientName(chosenAddress.getRecipientName());
            order.setRecipientPhone(chosenAddress.getPhoneNumber());
            // order.setShippingProvider(provider); // Cần logic chọn đơn vị vận chuyển
            order.setSubtotal(subtotal);
            order.setShippingFee(shippingFee);
            order.setDiscountAmount(discount);
            order.setGrandTotal(grandTotal);
            order.setNotes(notes);
            // transactionID, paidAt, completedAt sẽ được cập nhật sau

            // 7. LƯU ORDER (Lần 1 để lấy OrderID)
            savedOrder = orderRepository.saveAndFlush(order); // Lưu và lấy ID ngay
            final Integer finalOrderId = savedOrder.getOrderID(); // ID cho OrderDetail

            // 8. TẠO VÀ LƯU ORDER DETAILS (bao gồm TOPPINGS)
            List<OrderDetail> orderDetailList = new ArrayList<>();
            for (CartItem cartItem : itemsToOrder) {
                OrderDetail detail = new OrderDetail();
                detail.setOrder(savedOrder);
                detail.setVariant(cartItem.getVariant());
                detail.setQuantity(cartItem.getQuantity());
                detail.setUnitPrice(cartItem.getVariant().getPrice()); // Giá gốc variant

                BigDecimal itemSubtotal = cartItem.getVariant().getPrice(); // Bắt đầu tính subtotal cho item này
                List<OrderDetailTopping> detailToppings = new ArrayList<>();

                // Sao chép toppings từ CartItem sang OrderDetailTopping
                if (cartItem.getSelectedToppings() != null) {
                    for (com.alotra.entity.product.Topping topping : cartItem.getSelectedToppings()) {
                        OrderDetailTopping odt = new OrderDetailTopping();
                        // Tạo ID phức hợp
                        OrderDetailToppingId odtId = new OrderDetailToppingId();
                        odtId.setOrderDetailID(null); // Sẽ được set sau khi OrderDetail được lưu? Hoặc cần cách khác
                        odtId.setToppingID(topping.getToppingID());
                        odt.setId(odtId);
                        //--- Cần xem lại cách xử lý ID phức hợp khi lưu ---
                        // Tạm thời bỏ qua ID, set quan hệ trực tiếp
                        odt.setOrderDetail(detail); // Liên kết ngược
                        odt.setTopping(topping);
                        odt.setUnitPrice(topping.getAdditionalPrice()); // Giá topping lúc đặt hàng
                        detailToppings.add(odt);
                        itemSubtotal = itemSubtotal.add(topping.getAdditionalPrice()); // Cộng giá topping vào subtotal
                    }
                }
                detail.setSubtotal(itemSubtotal.multiply(BigDecimal.valueOf(cartItem.getQuantity()))); // Subtotal = (variant + toppings) * quantity
                detail.setToppings(detailToppings); // Gán danh sách topping cho detail
                orderDetailList.add(detail);
            }
            // Lưu OrderDetails (đã bao gồm Toppings nhờ CascadeType.ALL)
            orderDetailRepository.saveAll(orderDetailList);
             // === Cần kiểm tra lại CascadeType và cách lưu OrderDetailTopping với @EmbeddedId ===


            // 11. XỬ LÝ THANH TOÁN & CHUYỂN HƯỚNG
            if ("COD".equalsIgnoreCase(paymentMethod)) { // Dùng equalsIgnoreCase
                // Cập nhật trạng thái cho COD (Ví dụ: Chờ xác nhận)
                savedOrder.setOrderStatus("Pending"); // Hoặc giữ Pending tùy quy trình
                savedOrder.setPaymentStatus("Unpaid");
                orderRepository.save(savedOrder); // Lưu lại trạng thái mới
                redirectAttributes.addFlashAttribute("orderId", finalOrderId); // Gửi ID đơn hàng qua flash
                session.removeAttribute("selectedCheckoutItemIds");
                return "redirect:/order-success"; // Chuyển đến trang thành công
            } else if ("VNPay".equalsIgnoreCase(paymentMethod)) {
                // Giữ trạng thái Pending, Unpaid cho thanh toán online
                String paymentUrl = vnPayService.createPaymentUrl(savedOrder, request);
                return new RedirectView(paymentUrl); // Redirect sang VNPay
            }
             // Thêm xử lý cho Momo nếu có
            // else if ("Momo".equalsIgnoreCase(paymentMethod)) { ... }

            // Nếu phương thức thanh toán không hợp lệ
            redirectAttributes.addFlashAttribute("errorMessage", "Phương thức thanh toán không được hỗ trợ.");
            return "redirect:/checkout";


        } catch (ResponseStatusException | UsernameNotFoundException e) {
            // Lỗi xác thực -> Login
            if (session != null) session.removeAttribute("selectedCheckoutItemIds");
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException | IllegalStateException e) {
             // Lỗi nghiệp vụ (không tìm thấy, sai quyền, thiếu shop...)
             System.err.println("Lỗi nghiệp vụ khi đặt hàng: " + e.getMessage());
             e.printStackTrace();
             if (e instanceof EntityNotFoundException && e.getMessage().contains("Địa chỉ")) {
                 redirectAttributes.addFlashAttribute("errorMessage", "Địa chỉ đã chọn không hợp lệ.");
             } else {
                 redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đặt hàng: " + e.getMessage());
             }
             return "redirect:/checkout"; // Quay lại trang checkout báo lỗi
        } catch (Exception e) {
             // Lỗi hệ thống khác
             System.err.println("Lỗi hệ thống khi đặt hàng: " + e.getMessage());
             e.printStackTrace();
             redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi hệ thống khi đặt hàng. Vui lòng thử lại.");
             // Giữ session để user thử lại
             return "redirect:/checkout";
        }
    }

    // Trang thành công (Thêm ModelAttribute để nhận orderId)
    @GetMapping("/order-success")
    public String orderSuccessPage(Model model, @ModelAttribute("orderId") Integer orderId) {
        // Có thể dùng orderId để lấy lại thông tin đơn hàng và hiển thị nếu cần
        model.addAttribute("orderId", orderId);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("categories", categoryService.findAll());
        return "shop/order-success"; // View order-success.html
    }

    // Trang thất bại (Có thể thêm lý do thất bại)
    @GetMapping("/order-failed")
    public String orderFailedPage(Model model, @RequestParam(required = false) String reason) {
        model.addAttribute("reason", reason);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("categories", categoryService.findAll());
        return "shop/order-failed"; // View order-failed.html
    }
}