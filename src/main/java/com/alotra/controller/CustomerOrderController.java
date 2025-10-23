package com.alotra.controller;

import com.alotra.entity.order.Order;
import com.alotra.entity.user.Customer;
import com.alotra.entity.user.User;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.user.CustomerService;
import com.alotra.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user") // Tiền tố chung
public class CustomerOrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerService customerService;
    
    @Autowired private CartService cartService;
    
    @Autowired
    private CategoryService categoryService;
    
    
    @Autowired
    @Qualifier("userServiceImpl")
    private UserService userService;

    // === Hàm trợ giúp lấy Customer ===
    private Customer getCurrentCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        String username = auth.getName();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
        return customerService.findByUser(currentUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ khách hàng"));
    }
    
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
     * ENDPOINT CHÍNH ĐỂ XEM LỊCH SỬ ĐƠN HÀNG
     * Xử lý GET /user/orders
     */
    @GetMapping("/orders")
    public String showOrderHistory(Model model) {
        try {
            Customer customer = getCurrentCustomer();
            
            // 1. Lấy danh sách đơn hàng từ CSDL
            List<Order> orders = orderRepository.findByCustomerOrderByOrderDateDesc(customer);

            // 2. Đưa danh sách ra view
            model.addAttribute("orders", orders);
            
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            
            return "user/order_history"; // Trả về file HTML (sẽ tạo ở Bước 3.3)

        } catch (ResponseStatusException e) {
            return "redirect:/login"; // Bắt đăng nhập
        }
    }
    
    /**
     * === ENDPOINT MỚI: XEM CHI TIẾT ĐƠN HÀNG ===
     * Xử lý GET /user/orders/{id}
     */
    @GetMapping("/orders/{id}")
    public String showOrderDetail(@PathVariable("id") Integer orderId, Model model) {
        try {
            // 1. Lấy thông tin khách hàng
            Customer customer = getCurrentCustomer();
            
            // 2. Lấy đơn hàng từ CSDL bằng ID
            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isEmpty()) {
                // Không tìm thấy đơn hàng
                return "redirect:/user/orders?error=not_found";
            }
            
            Order order = orderOpt.get();

            // 3. KIỂM TRA BẢO MẬT (Rất quan trọng)
            // Đảm bảo khách hàng này CHỈ được xem đơn hàng của chính họ
            if (!order.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                // Nếu ID khách hàng của đơn hàng không khớp, ném lỗi
                throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này.");
            }
            
            // 4. Đưa đơn hàng chi tiết ra view
            model.addAttribute("order", order);
            
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            
            return "user/order_detail"; // Trả về file HTML (sẽ tạo ở Bước 2)

        } catch (ResponseStatusException e) {
            return "redirect:/login"; // Bắt đăng nhập
        } catch (AccessDeniedException e) {
            return "redirect:/user/orders?error=denied"; // Lỗi bảo mật
        }
    }
}