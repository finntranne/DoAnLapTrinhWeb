package com.alotra.controller;

import com.alotra.entity.product.Favorite;
import com.alotra.entity.product.Product;
import com.alotra.entity.user.User;
import com.alotra.repository.product.FavoriteRepository;
import com.alotra.repository.product.ProductRepository;
import com.alotra.service.user.UserService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/user/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserService userService;

    // Hàm trợ giúp lấy User (Giữ nguyên)
    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        String username = auth.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + username));
    }

    // *** ĐÃ XÓA HÀM getRefererUrl VÀ THAY BẰNG LOGIC CHUYỂN HƯỚNG MỚI ***

    /**
     * Xử lý thêm sản phẩm vào danh sách yêu thích
     * Endpoint: /user/favorites/add?productId={id}&orderId={id}
     */
    @GetMapping("/add")
    public String addToFavorite(@RequestParam("productId") Integer productId,
                                @RequestParam(value = "orderId", required = false) Integer orderId, // ✅ THÊM orderId
                                RedirectAttributes redirectAttributes) {

        try {
            User user = getCurrentAuthenticatedUser();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm."));

            // 1. Kiểm tra sản phẩm đã có trong danh sách yêu thích chưa
            if (!favoriteRepository.existsByUser_IdAndProduct_ProductID(user.getId(), productId)) {
                // 2. Thêm mới
                Favorite favorite = new Favorite();
                favorite.setUser(user);
                favorite.setProduct(product);
                favorite.setCreatedAt(LocalDateTime.now());
                
                favoriteRepository.save(favorite);

                redirectAttributes.addFlashAttribute("successMessage",
                    "Đã thêm sản phẩm '" + product.getProductName() + "' vào danh sách yêu thích.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "Sản phẩm này đã có trong danh sách yêu thích của bạn.");
            }

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            System.err.println("Error adding favorite: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm yêu thích.");
        }
        
        // ✅ LOGIC CHUYỂN HƯỚNG QUAY LẠI TRANG CHI TIẾT ĐƠN HÀNG
        if (orderId != null) {
            return "redirect:/user/orders/" + orderId; 
        }
        return "redirect:/user/favorites"; // Fallback: Quay về trang Yêu thích mặc định
    }

    /**
     * Xử lý xóa sản phẩm khỏi danh sách yêu thích
     * Endpoint: /user/favorites/remove?productId={id}&orderId={id}
     */
    @GetMapping("/remove")
    public String removeFromFavorite(@RequestParam("productId") Integer productId,
                                     @RequestParam(value = "orderId", required = false) Integer orderId, // ✅ THÊM orderId
                                     RedirectAttributes redirectAttributes) {

        try {
            User user = getCurrentAuthenticatedUser();

            // 1. Kiểm tra và xóa
            if (favoriteRepository.existsByUser_IdAndProduct_ProductID(user.getId(), productId)) {
                favoriteRepository.deleteByUser_IdAndProduct_ProductID(user.getId(), productId);

                redirectAttributes.addFlashAttribute("successMessage",
                    "Đã xóa sản phẩm khỏi danh sách yêu thích.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "Sản phẩm không có trong danh sách yêu thích của bạn.");
            }

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Error removing favorite: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa yêu thích.");
        }
        
        // ✅ LOGIC CHUYỂN HƯỚNG QUAY LẠI TRANG CHI TIẾT ĐƠN HÀNG
        if (orderId != null) {
            return "redirect:/user/orders/" + orderId;
        }
        return "redirect:/user/favorites"; // Fallback: Quay về trang Yêu thích mặc định
    }
}