package com.alotra.controller;

import com.alotra.entity.order.OrderDetail;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.Review;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderDetailRepository; // Cần tạo Repository này
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.product.ReviewRepository;
import com.alotra.service.cloudinary.CloudinaryService;
import com.alotra.service.user.UserService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/review")
public class ReviewController {

    @Autowired private ReviewRepository reviewRepository; //
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderDetailRepository orderDetailRepository; // Cần tạo
    @Autowired private UserService userService;
    @Autowired private CloudinaryService cloudinaryService;
    // Hàm trợ giúp lấy User (Tái sử dụng)
    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        String username = auth.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + username));
    }
    
    // Phương thức cần có trong OrderDetailRepository
    // public Optional<OrderDetail> findById(Integer id);

    /**
     * HIỂN THỊ FORM ĐÁNH GIÁ
     * Endpoint: /review/create?orderDetailId={id}
     */
    @GetMapping("/create")
    public String showReviewForm(@RequestParam("orderDetailId") Integer orderDetailId, 
                                 Model model, 
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser();

            OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chi tiết đơn hàng."));

            // 1. Kiểm tra quyền sở hữu OrderDetail
            if (!orderDetail.getOrder().getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Bạn không có quyền đánh giá sản phẩm này.");
            }
            
            // 2. Kiểm tra trạng thái đơn hàng (phải là Completed)
            if (!"Completed".equalsIgnoreCase(orderDetail.getOrder().getOrderStatus())) {
                 redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể đánh giá các đơn hàng đã Hoàn thành.");
                 return "redirect:/user/orders/" + orderDetail.getOrder().getOrderID();
            }

            // 3. Kiểm tra đã đánh giá chưa (Dùng phương thức đã có)
            if (reviewRepository.existsByOrderDetail_OrderDetailID(orderDetailId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã đánh giá sản phẩm này rồi.");
                return "redirect:/user/orders/" + orderDetail.getOrder().getOrderID();
            }

            // Tạo đối tượng Review rỗng để bind vào form
            Review review = new Review();
            review.setOrderDetail(orderDetail); // Gán OrderDetail để biết đang đánh giá cái gì
            
            model.addAttribute("review", review);
            model.addAttribute("orderDetail", orderDetail);
            model.addAttribute("product", orderDetail.getVariant().getProduct());

            return "review/review_form"; // Trả về view form đánh giá

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             return "redirect:/user/orders";
        }
    }

    /**
     * XỬ LÝ SUBMIT FORM ĐÁNH GIÁ
     * Endpoint: /review/save (POST)
     */
    @PostMapping("/save")
    public String saveReview(@ModelAttribute("review") Review review,
                             @RequestParam("mediaFiles") List<MultipartFile> mediaFiles, // ✅ NHẬN FILES
                             RedirectAttributes redirectAttributes) {

        Integer orderId = null;
        List<String> uploadedUrls = new ArrayList<>();

        try {
            User user = getCurrentAuthenticatedUser();
            
            // Lấy OrderDetail từ ID trong form
            OrderDetail orderDetail = orderDetailRepository.findById(review.getOrderDetail().getOrderDetailID())
                    .orElseThrow(() -> new EntityNotFoundException("Chi tiết đơn hàng không hợp lệ."));

            orderId = orderDetail.getOrder().getOrderID();

            // Kiểm tra và đặt các giá trị cần thiết
            if (!orderDetail.getOrder().getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Bạn không có quyền đánh giá sản phẩm này.");
            }
            
            // 1. Upload files lên Cloudinary
            if (mediaFiles != null && !mediaFiles.get(0).isEmpty()) {
                
                // Lọc bỏ file rỗng (khi người dùng không chọn file)
                List<MultipartFile> validFiles = mediaFiles.stream()
                        .filter(file -> !file.isEmpty())
                        .collect(Collectors.toList());

                for (MultipartFile file : validFiles) {
                    String contentType = file.getContentType();
                    String url = null;
                    
                    if (contentType != null && contentType.startsWith("image/")) {
                        // Upload ảnh, truyền User ID để lưu vào CloudinaryAsset
                        url = cloudinaryService.uploadImage(file, "reviews", user.getId()); 
                    } else if (contentType != null && contentType.startsWith("video/")) {
                        // Upload video, truyền User ID để lưu vào CloudinaryAsset
                        url = cloudinaryService.uploadVideo(file, "reviews", user.getId());
                    } else {
                        // Bỏ qua hoặc ném lỗi nếu là định dạng không hợp lệ khác
                        System.err.println("File type not supported: " + contentType);
                        continue; 
                    }
                    
                    if (url != null) {
                        uploadedUrls.add(url);
                    }
                }
                
                // Gán danh sách URL (cách nhau bởi dấu phẩy)
                review.setMediaURLs(String.join(",", uploadedUrls));
            }
            
            // 2. Gán đầy đủ các Entity cần thiết cho Review
            review.setUser(user);
            review.setProduct(orderDetail.getVariant().getProduct());
            review.setReviewDate(LocalDateTime.now());
            review.setIsVerifiedPurchase(true);

            // 3. Lưu Review
            reviewRepository.save(review);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Cảm ơn bạn đã đánh giá sản phẩm '" + review.getProduct().getProductName() + "'.");
            
            return "redirect:/user/orders/" + orderId;

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // Quay lại trang chi tiết đơn hàng
            return "redirect:/user/orders" + (orderId != null ? "/" + orderId : "");
        } catch (RuntimeException e) { // Bắt các lỗi từ CloudinaryService
            System.err.println("Error saving review (including file upload): " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu đánh giá hoặc upload file.");
            return "redirect:/user/orders" + (orderId != null ? "/" + orderId : "");
        }
    }
}