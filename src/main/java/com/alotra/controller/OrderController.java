package com.alotra.controller;

import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderItem;
import com.alotra.entity.user.Address;
import com.alotra.entity.user.Customer;
import com.alotra.entity.user.User;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.order.OrderItemRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.user.AddressRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.checkout.VNPayService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.user.CustomerService;
import com.alotra.service.user.UserService; // <-- Import mới

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier; // <-- Import mới
import org.springframework.http.HttpStatus; // <-- Import mới
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication; // <-- Import mới
import org.springframework.security.core.context.SecurityContextHolder; // <-- Import mới
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // <-- Import mới
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException; // <-- Import mới
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List; // <-- Import mới
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class OrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private VNPayService vnPayService;
    @Autowired private CartService cartService;
    @Autowired private CartRepository cartRepository;
    @Autowired private AddressRepository customerAddressRepository; // Repo địa chỉ
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    // --- Bổ sung các Service giống CartController ---
    @Autowired private CustomerService customerService;
    
    @Autowired
    @Qualifier("userServiceImpl") // Giống CartController
    private UserService userService;

    // === HÀM TRỢ GIÚP: Lấy Customer (Giống logic của CartController) ===
    /**
     * Lấy thông tin Customer đang đăng nhập.
     * Ném Exception nếu không tìm thấy, giống logic trong CartController.
     */
    private Customer getCurrentCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục");
        }
        String username = auth.getName();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
        return customerService.findByUser(currentUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ khách hàng"));
    }
    // =================================================================
    
    // --- Hàm trợ giúp lấy số lượng giỏ hàng ---
    private int getCurrentCartItemCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            Optional<User> userOpt = userService.findByUsername(username); // Hoặc findByEmail
            if (userOpt.isPresent()) {
                Optional<Customer> customerOpt = customerService.findByUser(userOpt.get());
                if (customerOpt.isPresent()) {
                    return cartService.getCartItemCount(customerOpt.get());
                }
            }
        }
        return 0; // Trả về 0 nếu chưa đăng nhập hoặc có lỗi
    }
    
    /**
     * === HÀM MỚI: XỬ LÝ VIỆC CHỌN ITEM TỪ GIỎ HÀNG ===
     * Nhận POST từ form trong cart.html
     */
    @PostMapping("/cart/select-for-checkout")
    public String selectItemsForCheckout(@RequestParam(name = "selectedItemIds", required = false) List<Long> selectedItemIds,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        
        // 1. Kiểm tra xem có chọn item nào không
        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một sản phẩm để thanh toán.");
            return "redirect:/cart";
        }
        
        // 2. Xác thực item (đảm bảo item này thuộc về user)
        try {
            Customer customer = getCurrentCustomer();
            Cart cart = cartRepository.findByCustomer(customer)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

            // Lấy TẤT CẢ item ID hợp lệ của user
            Set<Long> userCartItemIds = cart.getItems().stream()
                    .map(CartItem::getCartItemId)
                    .map(Long::valueOf) // <-- ĐÃ SỬA: Chuyển Integer sang Long
                    .collect(Collectors.toSet());

            // Lọc ra danh sách ID hợp lệ mà user đã chọn
            List<Long> validSelectedIds = selectedItemIds.stream()
                    .filter(userCartItemIds::contains)
                    .collect(Collectors.toList());

            if (validSelectedIds.isEmpty()) {
                 redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm đã chọn không hợp lệ.");
                 return "redirect:/cart";
            }
            
            // 3. Lưu danh sách ID hợp lệ vào Session
            session.setAttribute("selectedCheckoutItemIds", validSelectedIds);
            
            // 4. Chuyển hướng đến trang checkout
            return "redirect:/checkout";

        } catch (ResponseStatusException e) {
            return "redirect:/login"; // Chưa đăng nhập
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
             return "redirect:/cart";
        }
    }
    

    /**
     * HIỂN THỊ TRANG CHECKOUT (ĐÃ SỬA ĐỔI)
     * Sẽ đọc danh sách Item ID từ Session
     */
    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, HttpSession session) { // <-- Thêm HttpSession
        
        try {
            // === 1. LẤY ID TỪ SESSION ===
            @SuppressWarnings("unchecked")
            List<Long> selectedItemIds = (List<Long>) session.getAttribute("selectedCheckoutItemIds");
            
            // Nếu không có ID trong session (ví dụ: gõ URL trực tiếp), trả về giỏ hàng
            if (selectedItemIds == null || selectedItemIds.isEmpty()) {
                return "redirect:/cart?error=no_items_selected"; 
            }

            // 2. Lấy thông tin Customer
            Customer customer = getCurrentCustomer();
            
            // 3. Lấy giỏ hàng VÀ LỌC
            Cart cart = cartRepository.findByCustomer(customer)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));
            
            // Lọc danh sách items CHỈ BAO GỒM những cái đã chọn
            List<CartItem> itemsToCheckout = cart.getItems().stream()
                    .filter(item -> selectedItemIds.contains(Long.valueOf(item.getCartItemId()))) // <-- ĐÃ SỬA: Chuyển sang Long
                    .collect(Collectors.toList());

            // Nếu list rỗng (ví dụ: item đã bị xóa ở tab khác)
            if (itemsToCheckout.isEmpty()) {
                 session.removeAttribute("selectedCheckoutItemIds"); // Xóa session
                 return "redirect:/cart?error=items_not_found";
            }
            
            // 4. TÍNH TOÁN LẠI TỔNG TIỀN (CHỈ DỰA TRÊN CÁC MỤC ĐÃ LỌC)
            BigDecimal subtotal = BigDecimal.ZERO;
            for (CartItem item : itemsToCheckout) {
                BigDecimal itemPrice = item.getVariant().getPrice();
                Set<com.alotra.entity.product.Topping> toppings = item.getSelectedToppings();
                if (toppings != null && !toppings.isEmpty()) {
                    for (com.alotra.entity.product.Topping topping : toppings) {
                        if (topping != null && topping.getAdditionalPrice() != null) {
                            itemPrice = itemPrice.add(topping.getAdditionalPrice());
                        }
                    }
                }
                BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(lineTotal);
            }
            // =========================================================

            BigDecimal shippingFee = new BigDecimal(30000); // Ví dụ
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);

            // 5. Lấy danh sách địa chỉ
            List<Address> addresses = customerAddressRepository.findByCustomer(customer);
            
            // 6. Đưa thông tin ĐÃ LỌC ra View
            model.addAttribute("cartItems", itemsToCheckout); // <-- DANH SÁCH ĐÃ LỌC
            model.addAttribute("subtotal", subtotal); // <-- TỔNG TIỀN ĐÃ TÍNH LẠI
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("grandTotal", grandTotal);
            model.addAttribute("addresses", addresses); 
            model.addAttribute("customer", customer);
            
            model.addAttribute("cartItemCount", getCurrentCartItemCount()); // Cái này vẫn là tổng giỏ hàng
            model.addAttribute("categories", categoryService.findAll());

            // 7. Trả về tên file HTML
            return "shop/checkout"; 

        } catch (ResponseStatusException e) {
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Lỗi khi tải trang checkout: " + e.getMessage());
            e.printStackTrace();
            session.removeAttribute("selectedCheckoutItemIds"); // Xóa session nếu lỗi
            return "redirect:/cart?error=checkout_failed";
        }
    }

    /**
     * XỬ LÝ ĐẶT HÀNG (ĐÃ SỬA ĐỔI)
     * Cũng sẽ đọc Item ID từ Session
     */
    @PostMapping("/place-order")
    @Transactional 
    public Object placeOrder(
            @RequestParam(required = false) Integer addressId,
            @RequestParam(required = false) String notes,
            @RequestParam String paymentMethod,
            HttpServletRequest request,
            HttpSession session) { // <-- Thêm HttpSession

        if (addressId == null) {
            // Thêm session.keepAttributes("selectedCheckoutItemIds"); nếu bạn dùng RedirectAttributes
            return "redirect:/checkout?error=no_address_selected";
        }

        Order savedOrder = null; 

        try {
            // === 1. LẤY ID TỪ SESSION ===
            @SuppressWarnings("unchecked")
            List<Long> selectedItemIds = (List<Long>) session.getAttribute("selectedCheckoutItemIds");

            if (selectedItemIds == null || selectedItemIds.isEmpty()) {
                return "redirect:/cart?error=session_expired"; 
            }

            // 2. Lấy thông tin Customer
            Customer customer = getCurrentCustomer();

            // 3. Lấy giỏ hàng VÀ LỌC (Giống hệt showCheckoutPage)
            Cart cart = cartRepository.findByCustomer(customer)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

            List<CartItem> itemsToOrder = cart.getItems().stream()
                    .filter(item -> selectedItemIds.contains(Long.valueOf(item.getCartItemId()))) // <-- ĐÃ SỬA: Chuyển sang Long
                    .collect(Collectors.toList());
            
            if (itemsToOrder.isEmpty()) {
                session.removeAttribute("selectedCheckoutItemIds");
                return "redirect:/cart?error=items_not_found";
            }

            // 4. TÍNH TOÁN LẠI TỔNG TIỀN (Giống hệt showCheckoutPage)
            BigDecimal subtotal = BigDecimal.ZERO;
            for (CartItem item : itemsToOrder) {
                // ... (Copy y hệt logic tính tổng tiền từ showCheckoutPage) ...
                BigDecimal itemPrice = item.getVariant().getPrice();
                 Set<com.alotra.entity.product.Topping> toppings = item.getSelectedToppings();
                 if (toppings != null && !toppings.isEmpty()) {
                     for (com.alotra.entity.product.Topping topping : toppings) {
                         if (topping != null && topping.getAdditionalPrice() != null) {
                             itemPrice = itemPrice.add(topping.getAdditionalPrice());
                         }
                     }
                 }
                BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(lineTotal);
            }
            BigDecimal shippingFee = new BigDecimal(30000);
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);

            // 5. Lấy địa chỉ
            Address chosenAddress = customerAddressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ"));
            if (!chosenAddress.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                throw new AccessDeniedException("Bạn không có quyền sử dụng địa chỉ này.");
            }

            // 6. TẠO VÀ LƯU ORDER (Dùng tổng tiền ĐÃ TÍNH LẠI)
            Order order = new Order();
            
            // === ĐÃ SỬA: BỔ SUNG CÁC TRƯỜNG BỊ THIẾU ===
            order.setCustomer(customer);
            order.setOrderDate(Instant.now());
            order.setPaymentMethod(paymentMethod);
            order.setNotes(notes);
            order.setRecipientName(chosenAddress.getRecipientName());
            order.setRecipientPhone(chosenAddress.getPhoneNumber());
            order.setShippingAddress(chosenAddress.getFullAddress());
            
            order.setSubtotal(subtotal); // <-- TỔNG TIỀN MỚI
            order.setShippingFee(shippingFee);
            order.setDiscountAmount(discount);
            order.setGrandTotal(grandTotal); // <-- TỔNG TIỀN MỚI

            // === ĐÃ SỬA: BỔ SUNG LOGIC SET STATUS (FIX LỖI NULL) ===
            if ("Cash".equals(paymentMethod)) {
                order.setOrderStatus("Processing");
                order.setPaymentStatus("Unpaid");
            } else { // VNPay, Momo...
                order.setOrderStatus("Pending");
                order.setPaymentStatus("Unpaid");
            }
            // =====================================================

            savedOrder = orderRepository.saveAndFlush(order); 

            // 7. LẶP QUA CART VÀ LƯU TỪNG ORDER ITEM
            // === THAY ĐỔI VÒNG LẶP: CHỈ LẶP QUA itemsToOrder ===
            for (CartItem cartItem : itemsToOrder) { 
                
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder); 
                orderItem.setVariant(cartItem.getVariant());
                orderItem.setQuantity(cartItem.getQuantity());

                // Tính giá (variant + toppings)
                BigDecimal itemPrice = cartItem.getVariant().getPrice();
                 Set<com.alotra.entity.product.Topping> toppings = cartItem.getSelectedToppings(); // Lấy set topping ra biến
                 if (toppings != null && !toppings.isEmpty()) {
                     for (com.alotra.entity.product.Topping topping : toppings) {
                         if (topping != null && topping.getAdditionalPrice() != null) {
                             itemPrice = itemPrice.add(topping.getAdditionalPrice());
                         }
                     }
                 }
                orderItem.setPrice(itemPrice);

                // Lưu tên toppings
                String toppingNames = "";
                if (toppings != null && !toppings.isEmpty()) {
                    toppingNames = toppings.stream()
                            .filter(t -> t != null && t.getToppingName() != null)
                            .map(com.alotra.entity.product.Topping::getToppingName)
                            .collect(Collectors.joining(", "));
                }
                orderItem.setToppingsSnapshot(toppingNames);

                orderItemRepository.save(orderItem); 
            }
            
            // 8. XỬ LÝ SAU KHI LƯU
            // === THAY ĐỔI: XÓA CÁC ITEM ĐÃ CHỌN, KHÔNG XÓA HẾT ===
            cart.getItems().removeAll(itemsToOrder); 
            cartRepository.save(cart); // Lưu lại giỏ hàng

            // Xóa session
            session.removeAttribute("selectedCheckoutItemIds");

            // 9. CHUYỂN HƯỚNG
            if ("Cash".equals(paymentMethod)) {
                return "redirect:/order-success";
            } else if ("VNPay".equals(paymentMethod)) {
                String paymentUrl = vnPayService.createPaymentUrl(savedOrder, request);
                return new RedirectView(paymentUrl);
            }

            return "redirect:/checkout?error=Unknown payment method";

        } catch (ResponseStatusException e) { 
            session.removeAttribute("selectedCheckoutItemIds");
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
             System.err.println("Lỗi nghiệp vụ khi đặt hàng: " + e.getMessage());
             e.printStackTrace();
             // Giữ session để user thử lại
             return "redirect:/checkout?error=address_invalid";
        } catch (Exception e) {
             System.err.println("Lỗi hệ thống khi đặt hàng: " + e.getMessage());
             e.printStackTrace();
             // Giữ session để user thử lại
             return "redirect:/checkout?error=payment_error";
        }
    }

    // Trang thành công (Không đổi)
    @GetMapping("/order-success")
    public String orderSuccessPage() {
        return "shop/order-success"; 
    }

    // Trang thất bại (Không đổi)
    @GetMapping("/order-failed")
    public String orderFailedPage() {
        return "shop/order-failed";
    }
}