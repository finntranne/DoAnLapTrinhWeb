//package com.alotra.controller;
//
//import com.alotra.entity.cart.Cart;
//import com.alotra.entity.cart.CartItem;
//import com.alotra.entity.order.Order;
//import com.alotra.entity.order.OrderItem;
//import com.alotra.entity.user.Address;
//import com.alotra.entity.user.Customer;
//import com.alotra.entity.user.User;
//import com.alotra.repository.cart.CartRepository;
//import com.alotra.repository.order.OrderItemRepository;
//import com.alotra.repository.order.OrderRepository;
//import com.alotra.repository.user.AddressRepository;
//import com.alotra.service.cart.CartService;
//import com.alotra.service.checkout.VNPayService;
//import com.alotra.service.product.CategoryService;
//import com.alotra.service.user.CustomerService;
//import com.alotra.service.user.UserService; // <-- Import mới
//
//import jakarta.persistence.EntityNotFoundException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpSession;
//import jakarta.transaction.Transactional;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier; // <-- Import mới
//import org.springframework.http.HttpStatus; // <-- Import mới
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.core.Authentication; // <-- Import mới
//import org.springframework.security.core.context.SecurityContextHolder; // <-- Import mới
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model; // <-- Import mới
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.server.ResponseStatusException; // <-- Import mới
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//import org.springframework.web.servlet.view.RedirectView;
//
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.List; // <-- Import mới
//import java.util.Optional;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Controller
//public class OrderController {
//
//    @Autowired private OrderRepository orderRepository;
//    @Autowired private VNPayService vnPayService;
//    @Autowired private CartService cartService;
//    @Autowired private CartRepository cartRepository;
//    @Autowired private AddressRepository customerAddressRepository; // Repo địa chỉ
//    
//    @Autowired
//    private CategoryService categoryService;
//    
//    @Autowired
//    private OrderItemRepository orderItemRepository;
//    
//    // --- Bổ sung các Service giống CartController ---
//    @Autowired private CustomerService customerService;
//    
//    @Autowired
//    @Qualifier("userServiceImpl") // Giống CartController
//    private UserService userService;
//
//    // === HÀM TRỢ GIÚP: Lấy Customer (Giống logic của CartController) ===
//    /**
//     * Lấy thông tin Customer đang đăng nhập.
//     * Ném Exception nếu không tìm thấy, giống logic trong CartController.
//     */
//    private Customer getCurrentCustomer() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục");
//        }
//        String username = auth.getName();
//        User currentUser = userService.findByUsername(username)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
//        return customerService.findByUser(currentUser)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ khách hàng"));
//    }
//    // =================================================================
//    
//    // --- Hàm trợ giúp lấy số lượng giỏ hàng ---
//    private int getCurrentCartItemCount() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
//            String username = auth.getName();
//            Optional<User> userOpt = userService.findByUsername(username); // Hoặc findByEmail
//            if (userOpt.isPresent()) {
//                Optional<Customer> customerOpt = customerService.findByUser(userOpt.get());
//                if (customerOpt.isPresent()) {
//                    return cartService.getCartItemCount(customerOpt.get());
//                }
//            }
//        }
//        return 0; // Trả về 0 nếu chưa đăng nhập hoặc có lỗi
//    }
//    
//    /**
//     * === HÀM MỚI: XỬ LÝ VIỆC CHỌN ITEM TỪ GIỎ HÀNG ===
//     * Nhận POST từ form trong cart.html
//     */
//    @PostMapping("/cart/select-for-checkout")
//    public String selectItemsForCheckout(@RequestParam(name = "selectedItemIds", required = false) List<Long> selectedItemIds,
//                                         HttpSession session,
//                                         RedirectAttributes redirectAttributes) {
//        
//        // 1. Kiểm tra xem có chọn item nào không
//        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một sản phẩm để thanh toán.");
//            return "redirect:/cart";
//        }
//        
//        // 2. Xác thực item (đảm bảo item này thuộc về user)
//        try {
//            Customer customer = getCurrentCustomer();
//            Cart cart = cartRepository.findByCustomer(customer)
//                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));
//
//            // Lấy TẤT CẢ item ID hợp lệ của user
//            Set<Long> userCartItemIds = cart.getItems().stream()
//                    .map(CartItem::getCartItemId)
//                    .map(Long::valueOf) // <-- ĐÃ SỬA: Chuyển Integer sang Long
//                    .collect(Collectors.toSet());
//
//            // Lọc ra danh sách ID hợp lệ mà user đã chọn
//            List<Long> validSelectedIds = selectedItemIds.stream()
//                    .filter(userCartItemIds::contains)
//                    .collect(Collectors.toList());
//
//            if (validSelectedIds.isEmpty()) {
//                 redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm đã chọn không hợp lệ.");
//                 return "redirect:/cart";
//            }
//            
//            // 3. Lưu danh sách ID hợp lệ vào Session
//            session.setAttribute("selectedCheckoutItemIds", validSelectedIds);
//            
//            // 4. Chuyển hướng đến trang checkout
//            return "redirect:/checkout";
//
//        } catch (ResponseStatusException e) {
//            return "redirect:/login"; // Chưa đăng nhập
//        } catch (Exception e) {
//             redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
//             return "redirect:/cart";
//        }
//    }
//    
//
//    /**
//     * HIỂN THỊ TRANG CHECKOUT (ĐÃ SỬA ĐỔI)
//     * Sẽ đọc danh sách Item ID từ Session
//     */
//    @GetMapping("/checkout")
//    public String showCheckoutPage(Model model, HttpSession session) { // <-- Thêm HttpSession
//        
//        try {
//            // === 1. LẤY ID TỪ SESSION ===
//            @SuppressWarnings("unchecked")
//            List<Long> selectedItemIds = (List<Long>) session.getAttribute("selectedCheckoutItemIds");
//            
//            // Nếu không có ID trong session (ví dụ: gõ URL trực tiếp), trả về giỏ hàng
//            if (selectedItemIds == null || selectedItemIds.isEmpty()) {
//                return "redirect:/cart?error=no_items_selected"; 
//            }
//
//            // 2. Lấy thông tin Customer
//            Customer customer = getCurrentCustomer();
//            
//            // 3. Lấy giỏ hàng VÀ LỌC
//            Cart cart = cartRepository.findByCustomer(customer)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));
//            
//            // Lọc danh sách items CHỈ BAO GỒM những cái đã chọn
//            List<CartItem> itemsToCheckout = cart.getItems().stream()
//                    .filter(item -> selectedItemIds.contains(Long.valueOf(item.getCartItemId()))) // <-- ĐÃ SỬA: Chuyển sang Long
//                    .collect(Collectors.toList());
//
//            // Nếu list rỗng (ví dụ: item đã bị xóa ở tab khác)
//            if (itemsToCheckout.isEmpty()) {
//                 session.removeAttribute("selectedCheckoutItemIds"); // Xóa session
//                 return "redirect:/cart?error=items_not_found";
//            }
//            
//            // 4. TÍNH TOÁN LẠI TỔNG TIỀN (CHỈ DỰA TRÊN CÁC MỤC ĐÃ LỌC)
//            BigDecimal subtotal = BigDecimal.ZERO;
//            for (CartItem item : itemsToCheckout) {
//                BigDecimal itemPrice = item.getVariant().getPrice();
//                Set<com.alotra.entity.product.Topping> toppings = item.getSelectedToppings();
//                if (toppings != null && !toppings.isEmpty()) {
//                    for (com.alotra.entity.product.Topping topping : toppings) {
//                        if (topping != null && topping.getAdditionalPrice() != null) {
//                            itemPrice = itemPrice.add(topping.getAdditionalPrice());
//                        }
//                    }
//                }
//                BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
//                subtotal = subtotal.add(lineTotal);
//            }
//            // =========================================================
//
//            BigDecimal shippingFee = new BigDecimal(30000); // Ví dụ
//            BigDecimal discount = BigDecimal.ZERO;
//            BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);
//
//            // 5. Lấy danh sách địa chỉ
//            List<Address> addresses = customerAddressRepository.findByCustomer(customer);
//            
//            // 6. Đưa thông tin ĐÃ LỌC ra View
//            model.addAttribute("cartItems", itemsToCheckout); // <-- DANH SÁCH ĐÃ LỌC
//            model.addAttribute("subtotal", subtotal); // <-- TỔNG TIỀN ĐÃ TÍNH LẠI
//            model.addAttribute("shippingFee", shippingFee);
//            model.addAttribute("grandTotal", grandTotal);
//            model.addAttribute("addresses", addresses); 
//            model.addAttribute("customer", customer);
//            
//            model.addAttribute("cartItemCount", getCurrentCartItemCount()); // Cái này vẫn là tổng giỏ hàng
//            model.addAttribute("categories", categoryService.findAll());
//
//            // 7. Trả về tên file HTML
//            return "shop/checkout"; 
//
//        } catch (ResponseStatusException e) {
//            return "redirect:/login";
//        } catch (Exception e) {
//            System.err.println("Lỗi khi tải trang checkout: " + e.getMessage());
//            e.printStackTrace();
//            session.removeAttribute("selectedCheckoutItemIds"); // Xóa session nếu lỗi
//            return "redirect:/cart?error=checkout_failed";
//        }
//    }
//
//    /**
//     * XỬ LÝ ĐẶT HÀNG (ĐÃ SỬA ĐỔI)
//     * Cũng sẽ đọc Item ID từ Session
//     */
//    @PostMapping("/place-order")
//    @Transactional 
//    public Object placeOrder(
//            @RequestParam(required = false) Integer addressId,
//            @RequestParam(required = false) String notes,
//            @RequestParam String paymentMethod,
//            HttpServletRequest request,
//            HttpSession session) { // <-- Thêm HttpSession
//
//        if (addressId == null) {
//            // Thêm session.keepAttributes("selectedCheckoutItemIds"); nếu bạn dùng RedirectAttributes
//            return "redirect:/checkout?error=no_address_selected";
//        }
//
//        Order savedOrder = null; 
//
//        try {
//            // === 1. LẤY ID TỪ SESSION ===
//            @SuppressWarnings("unchecked")
//            List<Long> selectedItemIds = (List<Long>) session.getAttribute("selectedCheckoutItemIds");
//
//            if (selectedItemIds == null || selectedItemIds.isEmpty()) {
//                return "redirect:/cart?error=session_expired"; 
//            }
//
//            // 2. Lấy thông tin Customer
//            Customer customer = getCurrentCustomer();
//
//            // 3. Lấy giỏ hàng VÀ LỌC (Giống hệt showCheckoutPage)
//            Cart cart = cartRepository.findByCustomer(customer)
//                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));
//
//            List<CartItem> itemsToOrder = cart.getItems().stream()
//                    .filter(item -> selectedItemIds.contains(Long.valueOf(item.getCartItemId()))) // <-- ĐÃ SỬA: Chuyển sang Long
//                    .collect(Collectors.toList());
//            
//            if (itemsToOrder.isEmpty()) {
//                session.removeAttribute("selectedCheckoutItemIds");
//                return "redirect:/cart?error=items_not_found";
//            }
//
//            // 4. TÍNH TOÁN LẠI TỔNG TIỀN (Giống hệt showCheckoutPage)
//            BigDecimal subtotal = BigDecimal.ZERO;
//            for (CartItem item : itemsToOrder) {
//                // ... (Copy y hệt logic tính tổng tiền từ showCheckoutPage) ...
//                BigDecimal itemPrice = item.getVariant().getPrice();
//                 Set<com.alotra.entity.product.Topping> toppings = item.getSelectedToppings();
//                 if (toppings != null && !toppings.isEmpty()) {
//                     for (com.alotra.entity.product.Topping topping : toppings) {
//                         if (topping != null && topping.getAdditionalPrice() != null) {
//                             itemPrice = itemPrice.add(topping.getAdditionalPrice());
//                         }
//                     }
//                 }
//                BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
//                subtotal = subtotal.add(lineTotal);
//            }
//            BigDecimal shippingFee = new BigDecimal(30000);
//            BigDecimal discount = BigDecimal.ZERO;
//            BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);
//
//            // 5. Lấy địa chỉ
//            Address chosenAddress = customerAddressRepository.findById(addressId)
//                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ"));
//            if (!chosenAddress.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
//                throw new AccessDeniedException("Bạn không có quyền sử dụng địa chỉ này.");
//            }
//
//            // 6. TẠO VÀ LƯU ORDER (Dùng tổng tiền ĐÃ TÍNH LẠI)
//            Order order = new Order();
//            
//            // === ĐÃ SỬA: BỔ SUNG CÁC TRƯỜNG BỊ THIẾU ===
//            order.setCustomer(customer);
//            order.setOrderDate(Instant.now());
//            order.setPaymentMethod(paymentMethod);
//            order.setNotes(notes);
//            order.setRecipientName(chosenAddress.getRecipientName());
//            order.setRecipientPhone(chosenAddress.getPhoneNumber());
//            order.setShippingAddress(chosenAddress.getFullAddress());
//            
//            order.setSubtotal(subtotal); // <-- TỔNG TIỀN MỚI
//            order.setShippingFee(shippingFee);
//            order.setDiscountAmount(discount);
//            order.setGrandTotal(grandTotal); // <-- TỔNG TIỀN MỚI
//
//            // === ĐÃ SỬA: BỔ SUNG LOGIC SET STATUS (FIX LỖI NULL) ===
//            if ("Cash".equals(paymentMethod)) {
//                order.setOrderStatus("Pending");
//                order.setPaymentStatus("Unpaid");
//            } else { // VNPay, Momo...
//                order.setOrderStatus("Pending");
//                order.setPaymentStatus("Unpaid");
//            }
//            // =====================================================
//
//            savedOrder = orderRepository.saveAndFlush(order); 
//
//            // 7. LẶP QUA CART VÀ LƯU TỪNG ORDER ITEM
//            // === THAY ĐỔI VÒNG LẶP: CHỈ LẶP QUA itemsToOrder ===
//            for (CartItem cartItem : itemsToOrder) { 
//                
//                OrderItem orderItem = new OrderItem();
//                orderItem.setOrder(savedOrder); 
//                orderItem.setVariant(cartItem.getVariant());
//                orderItem.setQuantity(cartItem.getQuantity());
//
//                // Tính giá (variant + toppings)
//                BigDecimal itemPrice = cartItem.getVariant().getPrice();
//                 Set<com.alotra.entity.product.Topping> toppings = cartItem.getSelectedToppings(); // Lấy set topping ra biến
//                 if (toppings != null && !toppings.isEmpty()) {
//                     for (com.alotra.entity.product.Topping topping : toppings) {
//                         if (topping != null && topping.getAdditionalPrice() != null) {
//                             itemPrice = itemPrice.add(topping.getAdditionalPrice());
//                         }
//                     }
//                 }
//                orderItem.setPrice(itemPrice);
//
//                // Lưu tên toppings
//                String toppingNames = "";
//                if (toppings != null && !toppings.isEmpty()) {
//                    toppingNames = toppings.stream()
//                            .filter(t -> t != null && t.getToppingName() != null)
//                            .map(com.alotra.entity.product.Topping::getToppingName)
//                            .collect(Collectors.joining(", "));
//                }
//                orderItem.setToppingsSnapshot(toppingNames);
//
//                orderItemRepository.save(orderItem); 
//            }
//            
//            // 8. XỬ LÝ SAU KHI LƯU
//            // === THAY ĐỔI: XÓA CÁC ITEM ĐÃ CHỌN, KHÔNG XÓA HẾT ===
//            cart.getItems().removeAll(itemsToOrder); 
//            cartRepository.save(cart); // Lưu lại giỏ hàng
//
//            // Xóa session
//            session.removeAttribute("selectedCheckoutItemIds");
//
//            // 9. CHUYỂN HƯỚNG
//            if ("Cash".equals(paymentMethod)) {
//                return "redirect:/order-success";
//            } else if ("VNPay".equals(paymentMethod)) {
//                String paymentUrl = vnPayService.createPaymentUrl(savedOrder, request);
//                return new RedirectView(paymentUrl);
//            }
//
//            return "redirect:/checkout?error=Unknown payment method";
//
//        } catch (ResponseStatusException e) { 
//            session.removeAttribute("selectedCheckoutItemIds");
//            return "redirect:/login";
//        } catch (EntityNotFoundException | AccessDeniedException e) {
//             System.err.println("Lỗi nghiệp vụ khi đặt hàng: " + e.getMessage());
//             e.printStackTrace();
//             // Giữ session để user thử lại
//             return "redirect:/checkout?error=address_invalid";
//        } catch (Exception e) {
//             System.err.println("Lỗi hệ thống khi đặt hàng: " + e.getMessage());
//             e.printStackTrace();
//             // Giữ session để user thử lại
//             return "redirect:/checkout?error=payment_error";
//        }
//    }
//
//    // Trang thành công (Không đổi)
//    @GetMapping("/order-success")
//    public String orderSuccessPage() {
//        return "shop/order-success"; 
//    }
//
//    // Trang thất bại (Không đổi)
//    @GetMapping("/order-failed")
//    public String orderFailedPage() {
//        return "shop/order-failed";
//    }
//}


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
            @RequestParam Integer addressId, // Địa chỉ giao hàng đã chọn
            @RequestParam(required = false) String notes, // Ghi chú
            @RequestParam String paymentMethod, // COD, VNPay, Momo...
            HttpServletRequest request, // Cần cho VNPay
            HttpSession session,
            RedirectAttributes redirectAttributes) {

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

            // 9. XÓA CART ITEMS ĐÃ ĐẶT HÀNG
            List<Integer> itemIdsToRemove = itemsToOrder.stream().map(CartItem::getCartItemID).collect(Collectors.toList());
            cartItemRepository.deleteAllById(itemIdsToRemove); // Xóa các CartItem đã chọn

            // 10. XÓA SESSION
            session.removeAttribute("selectedCheckoutItemIds");

            // 11. XỬ LÝ THANH TOÁN & CHUYỂN HƯỚNG
            if ("COD".equalsIgnoreCase(paymentMethod)) { // Dùng equalsIgnoreCase
                // Cập nhật trạng thái cho COD (Ví dụ: Chờ xác nhận)
                savedOrder.setOrderStatus("Confirmed"); // Hoặc giữ Pending tùy quy trình
                savedOrder.setPaymentStatus("Unpaid");
                orderRepository.save(savedOrder); // Lưu lại trạng thái mới
                redirectAttributes.addFlashAttribute("orderId", finalOrderId); // Gửi ID đơn hàng qua flash
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
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đặt hàng: " + e.getMessage());
             // Giữ session để user thử lại nếu là lỗi tạm thời? Hoặc xóa đi?
             // session.removeAttribute("selectedCheckoutItemIds");
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