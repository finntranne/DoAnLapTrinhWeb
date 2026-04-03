package com.alotra.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderItem;
import com.alotra.entity.order.OrderShippingHistory;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.OrderShippingHistoryRepository;
import com.alotra.repository.order.PaymentRepository;
import com.alotra.repository.product.FavoriteRepository;
import com.alotra.repository.product.ReviewRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.shop.StoreService;
import com.alotra.service.user.UserService;
import com.alotra.view.order.OrderView;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class CustomerOrderController {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderShippingHistoryRepository orderShippingHistoryRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private UserService userService;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private StoreService storeService;

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

    @GetMapping("/orders")
    public String showOrderHistory(Model model,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        try {
            Integer selectedShopId = getSelectedShopId(session);
            User user = getCurrentAuthenticatedUser();

            Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
            Page<Order> orderPage = (status != null && !status.isBlank())
                    ? orderRepository.findByUser_IdAndOrderStatusIgnoreCase(user.getId(), status, pageable)
                    : orderRepository.findByUser_Id(user.getId(), pageable);

            List<OrderView> orderViews = orderPage.getContent().stream()
                    .map(order -> OrderView.from(order, paymentRepository.findByOrder_OrderID(order.getOrderID()).orElse(null)))
                    .toList();
            Page<OrderView> orderViewPage = new PageImpl<>(orderViews, pageable, orderPage.getTotalElements());

            model.addAttribute("orderPage", orderViewPage);
            model.addAttribute("orders", orderViews);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", orderViewPage.getTotalPages());
            model.addAttribute("currentStatus", status);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

            return "user/order_history";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Khong the tai lich su don hang.");
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("orderPage", Page.empty());
            model.addAttribute("orders", List.of());
            return "user/order_history";
        }
    }

    @GetMapping("/orders/{id}")
    public String showOrderDetail(@PathVariable("id") Integer orderId,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        try {
            Integer selectedShopId = getSelectedShopId(session);
            User user = getCurrentAuthenticatedUser();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang #" + orderId));

            if (!order.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Ban khong co quyen xem don hang nay.");
            }

            if ("Completed".equalsIgnoreCase(order.getOrderStatus())) {
                Map<Integer, Boolean> reviewStatusMap = new HashMap<>();
                Map<Integer, Boolean> favoriteStatusMap = new HashMap<>();

                for (OrderItem item : order.getItems()) {
                    Integer orderItemId = item.getOrderItemId();
                    Integer productId = item.getVariant().getProduct().getProductID();

                    boolean isReviewed = Boolean.TRUE.equals(reviewRepository.existsByOrderItem_OrderItemId(orderItemId));
                    reviewStatusMap.put(orderItemId, isReviewed);

                    if (!favoriteStatusMap.containsKey(productId)) {
                        boolean isFavorited = Boolean.TRUE.equals(
                                favoriteRepository.existsByUser_IdAndProduct_ProductID(user.getId(), productId));
                        favoriteStatusMap.put(productId, isFavorited);
                    }
                }

                model.addAttribute("reviewStatusMap", reviewStatusMap);
                model.addAttribute("favoriteStatusMap", favoriteStatusMap);
            }

            List<OrderShippingHistory> shippingHistory =
                    orderShippingHistoryRepository.findByOrder_OrderIDOrderByTimestampDesc(orderId);

            model.addAttribute("order",
                    OrderView.from(order, paymentRepository.findByOrder_OrderID(orderId).orElse(null)));
            model.addAttribute("shippingHistory", shippingHistory);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

            return "user/order_detail";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/user/orders";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loi khi tai chi tiet don hang.");
            return "redirect:/user/orders";
        }
    }
}
