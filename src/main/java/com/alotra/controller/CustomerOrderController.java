package com.alotra.controller;

import com.alotra.entity.order.Order;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.user.UserService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // *** THÊM ***
import org.springframework.data.domain.PageRequest; // *** THÊM ***
import org.springframework.data.domain.Pageable; // *** THÊM ***
import org.springframework.data.domain.Sort; // *** THÊM ***
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

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class CustomerOrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartService cartService;
    @Autowired private CategoryService categoryService;
    @Autowired private UserService userService;

    // === Hàm trợ giúp lấy User (Giữ nguyên) ===
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

    /**
     * XEM LỊCH SỬ ĐƠN HÀNG (ĐÃ SỬA + Thêm Phân Trang)
     */
    @GetMapping("/orders")
    public String showOrderHistory(Model model,
                                   @RequestParam(name = "status", required = false) String status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) { // Mặc định 10 đơn/trang
        try {
            User user = getCurrentAuthenticatedUser();

            Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());

            // *** SỬA: Dùng phương thức Query mới (cần tạo trong OrderRepository) ***
            // Giả sử có phương thức findByUser_IdAndOptionalStatus
            Page<Order> orderPage;
            if (status != null && !status.isBlank()) {
                 // Cần tạo phương thức này trong OrderRepository
                orderPage = orderRepository.findByUser_IdAndOrderStatusIgnoreCase(user.getId(), status, pageable);
            } else {
                 // Cần tạo phương thức này trong OrderRepository
                orderPage = orderRepository.findByUser_Id(user.getId(), pageable);
            }
            // *** KẾT THÚC SỬA ***

            model.addAttribute("orderPage", orderPage); // Truyền Page object
            model.addAttribute("orders", orderPage.getContent()); // List cho trang hiện tại
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", orderPage.getTotalPages());
            model.addAttribute("currentStatus", status);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());

            return "user/order_history";

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
             System.err.println("Error loading order history: " + e.getMessage());
             model.addAttribute("errorMessage", "Không thể tải lịch sử đơn hàng.");
             // Truyền các thuộc tính cần thiết khác cho layout ngay cả khi lỗi
             model.addAttribute("cartItemCount", getCurrentCartItemCount());
             model.addAttribute("categories", categoryService.findAll());
             model.addAttribute("orderPage", Page.empty()); // Truyền Page rỗng để tránh lỗi Thymeleaf
             model.addAttribute("orders", List.of());
             return "user/order_history";
        }
    }

    /**
     * XEM CHI TIẾT ĐƠN HÀNG (ĐÃ SỬA)
     */
    @GetMapping("/orders/{id}")
    public String showOrderDetail(@PathVariable("id") Integer orderId, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser();
            Order order = orderRepository.findById(orderId)
                 .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng #" + orderId));

            // *** SỬA: Kiểm tra User ID ***
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
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             return "redirect:/user/orders";
        } catch (Exception e) {
            System.err.println("Error loading order detail: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải chi tiết đơn hàng.");
            return "redirect:/user/orders";
        }
    }

    /**
     * XỬ LÝ HỦY ĐƠN HÀNG (ĐÃ SỬA)
     */
    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer orderId, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser();
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng #" + orderId));

            // *** SỬA: Kiểm tra User ID ***
            if (!order.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Bạn không có quyền hủy đơn hàng này.");
            }

            // *** SỬA: Kiểm tra các trạng thái có thể hủy (dùng equalsIgnoreCase) ***
            String currentStatus = order.getOrderStatus();
            if ("Pending".equalsIgnoreCase(currentStatus) || "Confirmed".equalsIgnoreCase(currentStatus)) { // Giả sử Confirmed là trạng thái sau COD chờ duyệt
                order.setOrderStatus("Cancelled");

                if ("Paid".equalsIgnoreCase(order.getPaymentStatus())) {
                    order.setPaymentStatus("Refunded");
                    // TODO: Thêm logic gọi API hoàn tiền nếu cần
                }

                orderRepository.save(order);
                redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng #" + orderId + " thành công.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn hàng ở trạng thái '" + currentStatus + "'.");
            }

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            System.err.println("Error cancelling order: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy đơn hàng.");
        }

        return "redirect:/user/orders"; // Quay lại trang lịch sử
    }
}