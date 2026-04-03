package com.alotra.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.order.OrderItem;
import com.alotra.entity.product.Review;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderItemRepository;
import com.alotra.repository.product.ReviewRepository;
import com.alotra.service.cloudinary.CloudinaryService;
import com.alotra.service.user.UserService;

import jakarta.persistence.EntityNotFoundException;

@Controller
@RequestMapping("/review")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private CloudinaryService cloudinaryService;

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap.");
        }

        String username = auth.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung: " + username));
    }

    @GetMapping("/create")
    public String showReviewForm(@RequestParam(name = "orderItemId", required = false) Integer orderItemId,
            @RequestParam(name = "orderDetailId", required = false) Integer legacyOrderItemId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser();
            Integer resolvedOrderItemId = orderItemId != null ? orderItemId : legacyOrderItemId;
            if (resolvedOrderItemId == null) {
                throw new IllegalArgumentException("Thieu thong tin san pham can danh gia.");
            }

            OrderItem orderItem = orderItemRepository.findById(resolvedOrderItemId)
                    .orElseThrow(() -> new EntityNotFoundException("Khong tim thay chi tiet don hang."));

            if (!orderItem.getOrder().getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Ban khong co quyen danh gia san pham nay.");
            }

            if (!"Completed".equalsIgnoreCase(orderItem.getOrder().getOrderStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Chi co the danh gia cac don hang da hoan thanh.");
                return "redirect:/user/orders/" + orderItem.getOrder().getOrderID();
            }

            if (Boolean.TRUE.equals(reviewRepository.existsByOrderItem_OrderItemId(resolvedOrderItemId))) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ban da danh gia san pham nay roi.");
                return "redirect:/user/orders/" + orderItem.getOrder().getOrderID();
            }

            Review review = new Review();
            review.setOrderItem(orderItem);

            model.addAttribute("review", review);
            model.addAttribute("orderItem", orderItem);
            model.addAttribute("product", orderItem.getVariant().getProduct());
            return "review/review_form";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/user/orders";
        }
    }

    @PostMapping("/save")
    public String saveReview(@ModelAttribute("review") Review review,
            @RequestParam(name = "orderItemId", required = false) Integer orderItemId,
            @RequestParam(name = "orderDetail.orderDetailID", required = false) Integer legacyOrderItemId,
            @RequestParam(name = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            RedirectAttributes redirectAttributes) {

        Integer resolvedOrderId = null;
        try {
            User user = getCurrentAuthenticatedUser();

            Integer resolvedOrderItemId = orderItemId;
            if (resolvedOrderItemId == null && review.getOrderItem() != null) {
                resolvedOrderItemId = review.getOrderItem().getOrderItemId();
            }
            if (resolvedOrderItemId == null) {
                resolvedOrderItemId = legacyOrderItemId;
            }
            if (resolvedOrderItemId == null) {
                throw new IllegalArgumentException("Thieu thong tin chi tiet don hang.");
            }

            OrderItem orderItem = orderItemRepository.findById(resolvedOrderItemId)
                    .orElseThrow(() -> new EntityNotFoundException("Chi tiet don hang khong hop le."));
            resolvedOrderId = orderItem.getOrder().getOrderID();

            if (!orderItem.getOrder().getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Ban khong co quyen danh gia san pham nay.");
            }
            if (Boolean.TRUE.equals(reviewRepository.existsByOrderItem_OrderItemId(resolvedOrderItemId))) {
                throw new IllegalArgumentException("Ban da danh gia san pham nay roi.");
            }

            List<String> uploadedUrls = new ArrayList<>();
            if (mediaFiles != null) {
                for (MultipartFile file : mediaFiles) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }

                    String contentType = file.getContentType();
                    String url = null;
                    if (contentType != null && contentType.startsWith("image/")) {
                        url = cloudinaryService.uploadImage(file, "reviews", user.getId());
                    } else if (contentType != null && contentType.startsWith("video/")) {
                        url = cloudinaryService.uploadVideo(file, "reviews", user.getId());
                    }

                    if (url != null) {
                        uploadedUrls.add(url);
                    }
                }
            }

            review.setOrderItem(orderItem);
            review.setUser(user);
            review.setProduct(orderItem.getVariant().getProduct());
            review.setReviewDate(LocalDateTime.now());
            review.setIsVerifiedPurchase(true);
            if (!uploadedUrls.isEmpty()) {
                review.setMediaURLs(String.join(",", uploadedUrls));
            }

            reviewRepository.save(review);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cam on ban da danh gia san pham '" + review.getProduct().getProductName() + "'.");
            return "redirect:/user/orders/" + resolvedOrderId;
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/user/orders" + (resolvedOrderId != null ? "/" + resolvedOrderId : "");
        } catch (RuntimeException ex) {
            log.error("Error saving review", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Loi khi luu danh gia hoac upload file.");
            return "redirect:/user/orders" + (resolvedOrderId != null ? "/" + resolvedOrderId : "");
        }
    }
}
