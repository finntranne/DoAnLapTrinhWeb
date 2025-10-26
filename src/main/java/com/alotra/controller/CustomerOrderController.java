//package com.alotra.controller;
//
//import com.alotra.entity.order.Order;
//import com.alotra.entity.user.Customer;
//import com.alotra.entity.user.User;
//import com.alotra.repository.cart.CartRepository;
//import com.alotra.repository.order.OrderRepository;
//import com.alotra.service.cart.CartService;
//import com.alotra.service.product.CategoryService;
//import com.alotra.service.user.CustomerService;
//import com.alotra.service.user.UserService;
//
//// === IMPORT MỚI ===
//import jakarta.persistence.EntityNotFoundException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.http.HttpStatus;
//// === SỬA IMPORT NÀY ===
//import org.springframework.security.access.AccessDeniedException; 
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.server.ResponseStatusException;
//
//// === IMPORT MỚI ===
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//// === BỎ IMPORT CŨ NÀY: import java.nio.file.AccessDeniedException; ===
//import java.util.List;
//import java.util.Optional;
//
//@Controller
//@RequestMapping("/user") // Tiền tố chung
//public class CustomerOrderController {
//
//    @Autowired private OrderRepository orderRepository;
//    @Autowired private CustomerService customerService;
//    
//    @Autowired private CartService cartService;
//    
//    @Autowired
//    private CategoryService categoryService;
//    
//    
//    @Autowired
//    @Qualifier("userServiceImpl")
//    private UserService userService;
//
//    // === Hàm trợ giúp lấy Customer (Giữ nguyên) ===
//    private Customer getCurrentCustomer() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
//        }
//        String username = auth.getName();
//        User currentUser = userService.findByUsername(username)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
//        return customerService.findByUser(currentUser)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ khách hàng"));
//    }
//    
//    // --- Hàm trợ giúp lấy số lượng giỏ hàng (Giữ nguyên) ---
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
//     * ENDPOINT CHÍNH ĐỂ XEM LỊCH SỬ ĐƠN HÀNG
//     * Xử lý GET /user/orders
//     * === ĐÃ CẬP NHẬT ĐỂ LỌC THEO TRẠNG THÁI ===
//     */
//    @GetMapping("/orders")
//    public String showOrderHistory(Model model,
//                                   @RequestParam(name = "status", required = false) String status) { // <-- THÊM MỚI
//        try {
//            Customer customer = getCurrentCustomer();
//            
//            // 1. Lấy danh sách đơn hàng (lọc hoặc tất cả)
//            List<Order> orders;
//            if (status != null && !status.isEmpty()) {
//                // Lấy danh sách đã lọc (bạn cần thêm method này vào OrderRepository)
//                orders = orderRepository.findByCustomerAndOrderStatusOrderByOrderDateDesc(customer, status);
//            } else {
//                // Lấy tất cả
//                orders = orderRepository.findByCustomerOrderByOrderDateDesc(customer);
//            }
//
//            // 2. Đưa danh sách ra view
//            model.addAttribute("orders", orders);
//            model.addAttribute("currentStatus", status); // <-- THÊM MỚI (để tô sáng tab)
//            
//            model.addAttribute("cartItemCount", getCurrentCartItemCount());
//            model.addAttribute("categories", categoryService.findAll());
//            
//            return "user/order_history"; // Trả về file HTML (tên file của bạn)
//
//        } catch (ResponseStatusException e) {
//            return "redirect:/login"; // Bắt đăng nhập
//        }
//    }
//    
//    /**
//     * === ENDPOINT MỚI: XEM CHI TIẾT ĐƠN HÀNG ===
//     * Xử lý GET /user/orders/{id}
//     * === ĐÃ SỬA LỖI IMPORT AccessDeniedException ===
//     */
//    @GetMapping("/orders/{id}")
//    public String showOrderDetail(@PathVariable("id") Integer orderId, Model model) {
//        try {
//            // 1. Lấy thông tin khách hàng
//            Customer customer = getCurrentCustomer();
//            
//            // 2. Lấy đơn hàng từ CSDL bằng ID
//            Order order = orderRepository.findById(orderId)
//                 .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng."));
//
//            // 3. KIỂM TRA BẢO MẬT
//            if (!order.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
//                throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này."); // <-- Đã sửa
//            }
//            
//            // 4. Đưa đơn hàng chi tiết ra view
//            model.addAttribute("order", order);
//            
//            model.addAttribute("cartItemCount", getCurrentCartItemCount());
//            model.addAttribute("categories", categoryService.findAll());
//            
//            return "user/order_detail"; 
//
//        } catch (ResponseStatusException e) {
//            return "redirect:/login";
//        } catch (EntityNotFoundException e) {
//             return "redirect:/user/orders?error=not_found"; // Không tìm thấy
//        } catch (AccessDeniedException e) {
//            return "redirect:/user/orders?error=denied"; // Lỗi bảo mật
//        }
//    }
//
//    /**
//     * === ENDPOINT MỚI: XỬ LÝ HỦY ĐƠN HÀNG ===
//     * Xử lý POST /user/orders/cancel/{id}
//     */
//    @PostMapping("/orders/cancel/{id}")
//    public String cancelOrder(@PathVariable("id") Integer orderId, RedirectAttributes redirectAttributes) {
//        try {
//            Customer customer = getCurrentCustomer();
//            Order order = orderRepository.findById(orderId)
//                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng."));
//
//            // Kiểm tra bảo mật: Đơn hàng này phải của user
//            if (!order.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
//                throw new AccessDeniedException("Bạn không có quyền hủy đơn hàng này.");
//            }
//
//            // Kiểm tra logic: Chỉ cho hủy khi trạng thái là 'Pending' hoặc 'Processing'
//            if ("Pending".equals(order.getOrderStatus()) || "Processing".equals(order.getOrderStatus())) {
//                order.setOrderStatus("Cancelled"); // Đổi trạng thái
//                orderRepository.save(order);
//                redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng #" + orderId + " thành công.");
//            } else {
//                redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn hàng ở trạng thái này.");
//            }
//
//        } catch (ResponseStatusException e) {
//            return "redirect:/login"; // Chưa đăng nhập
//        } catch (EntityNotFoundException | AccessDeniedException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy đơn hàng.");
//        }
//        
//        // Quay lại trang lịch sử đơn hàng
//        return "redirect:/user/orders";
//    }
//}


package com.alotra.controller; // Giữ package này

// Import entity đã merge
import com.alotra.entity.order.Order;
import com.alotra.entity.user.User; // Sử dụng User

// Import Service và Repository đã merge
import com.alotra.repository.order.OrderRepository; // Sử dụng OrderRepository đã sửa
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.user.UserService; // Sử dụng UserService

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
// Bỏ Qualifier nếu không cần
import org.springframework.data.domain.Page; // *** THÊM Page ***
import org.springframework.data.domain.PageRequest; // *** THÊM PageRequest ***
import org.springframework.data.domain.Pageable; // *** THÊM Pageable ***
import org.springframework.data.domain.Sort; // *** THÊM Sort ***
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException; // Đã đúng
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Thêm exception này
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; // Sử dụng *
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user") // Giữ tiền tố chung
public class CustomerOrderController { // Tên class có thể giữ nguyên hoặc đổi

    @Autowired private OrderRepository orderRepository; // Giữ lại
    // @Autowired private CustomerService customerService; // *** BỎ CustomerService ***

    @Autowired private CartService cartService;
    @Autowired private CategoryService categoryService;

    // @Autowired @Qualifier("userServiceImpl") // Qualifier có thể không cần
    @Autowired private UserService userService; // *** GIỮ UserService ***

    // === Hàm trợ giúp lấy User (Giống CartController) ===
    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        String username = auth.getName(); // Thường là email
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + username));
    }

    // --- Hàm trợ giúp lấy số lượng giỏ hàng (Giống CartController, ĐÃ SỬA) ---
    private int getCurrentCartItemCount() {
        try {
            User user = getCurrentAuthenticatedUser();
            return cartService.getCartItemCount(user); // Gọi service với User
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return 0; // Trả về 0 nếu chưa đăng nhập hoặc có lỗi
        }
    }

    /**
     * XEM LỊCH SỬ ĐƠN HÀNG (ĐÃ SỬA + Thêm Phân Trang)
     * Xử lý GET /user/orders
     */
    @GetMapping("/orders")
    public String showOrderHistory(Model model,
                                   @RequestParam(name = "status", required = false) String status,
                                   // *** THÊM Tham số Phân trang ***
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) { // Mặc định 10 đơn/trang
        try {
            User user = getCurrentAuthenticatedUser(); // Lấy User

            // *** SỬA: Sử dụng Pageable và phương thức repository mới ***
            Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending()); // Sắp xếp mới nhất trước
            Page<Order> orderPage = orderRepository.findUserOrdersByStatus(user.getId(), status, pageable);

            model.addAttribute("orderPage", orderPage); // Truyền Page object ra view
            model.addAttribute("orders", orderPage.getContent()); // Truyền List đơn hàng của trang hiện tại
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", orderPage.getTotalPages());
            // *** KẾT THÚC SỬA ***

            model.addAttribute("currentStatus", status); // Giữ lại để tô sáng tab
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());

            return "user/order_history"; // View order_history.html

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
            // Thêm log lỗi
            // log.error("Error loading order history for user {}", user.getId(), e);
             model.addAttribute("errorMessage", "Không thể tải lịch sử đơn hàng.");
             return "user/order_history"; // Vẫn trả về trang nhưng báo lỗi
        }
    }

    /**
     * XEM CHI TIẾT ĐƠN HÀNG (ĐÃ SỬA)
     * Xử lý GET /user/orders/{id}
     */
    @GetMapping("/orders/{id}")
    public String showOrderDetail(@PathVariable("id") Integer orderId, Model model, RedirectAttributes redirectAttributes) { // Thêm redirectAttributes
        try {
            User user = getCurrentAuthenticatedUser(); // Lấy User
            Order order = orderRepository.findById(orderId)
                 .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng #" + orderId));

            // KIỂM TRA BẢO MẬT (So sánh User ID)
            // *** SỬA: So sánh User ID ***
            if (!order.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này.");
            }

            model.addAttribute("order", order);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());

            return "user/order_detail"; // View order_detail.html

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
             // Dùng RedirectAttributes để gửi thông báo lỗi về trang lịch sử
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             return "redirect:/user/orders";
        } catch (Exception e) {
            // Log lỗi
            // log.error("Error loading order detail {} for user {}", orderId, user.getId(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải chi tiết đơn hàng.");
            return "redirect:/user/orders";
        }
    }

    /**
     * XỬ LÝ HỦY ĐƠN HÀNG (ĐÃ SỬA)
     * Xử lý POST /user/orders/cancel/{id}
     */
    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer orderId, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser(); // Lấy User
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng #" + orderId));

            // Kiểm tra bảo mật (So sánh User ID)
            // *** SỬA: So sánh User ID ***
            if (!order.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Bạn không có quyền hủy đơn hàng này.");
            }

            // Kiểm tra logic: Chỉ cho hủy khi trạng thái là 'Pending' hoặc 'Confirmed'
            // *** SỬA: Dùng equalsIgnoreCase để an toàn hơn ***
            if ("Pending".equalsIgnoreCase(order.getOrderStatus()) || "Confirmed".equalsIgnoreCase(order.getOrderStatus())) {
                order.setOrderStatus("Cancelled"); // Đổi trạng thái

                // *** THÊM LOGIC: Nếu đã thanh toán -> Đổi thành Refunded ***
                if ("Paid".equalsIgnoreCase(order.getPaymentStatus())) {
                    order.setPaymentStatus("Refunded");
                    // Thêm logic gửi yêu cầu hoàn tiền thực tế nếu cần
                }
                // *** KẾT THÚC THÊM ***

                orderRepository.save(order);
                redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng #" + orderId + " thành công.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn hàng ở trạng thái '" + order.getOrderStatus() + "'.");
            }

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Log lỗi
            // log.error("Error cancelling order {} for user {}", orderId, user.getId(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy đơn hàng.");
        }

        // Quay lại trang lịch sử đơn hàng
        return "redirect:/user/orders";
    }
}