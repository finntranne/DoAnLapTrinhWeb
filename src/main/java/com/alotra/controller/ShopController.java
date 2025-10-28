package com.alotra.controller;


import com.alotra.dto.shop.ShopRegistrationDTO;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.service.shop.StoreService;
import com.alotra.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails; // Cần dùng UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shop")
public class ShopController {

    @Autowired private StoreService storeService;
    @Autowired private UserService userService;

    // --- Hàm trợ giúp lấy User ID đang đăng nhập ---
    private Integer getCurrentUserId(String username) {
         return userService.findByUsername(username).map(User::getId).orElse(null);
    }

    @GetMapping("/register")
    @PreAuthorize("isAuthenticated()") // Bắt buộc đăng nhập
    public String showRegistrationForm(Model model, RedirectAttributes redirectAttributes) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName().equals("anonymousUser")) {
             return "redirect:/login";
        }
        
        Integer userId;
        try {
            userId = getCurrentUserId(auth.getName());
        } catch (Exception e) {
             throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không thể xác định người dùng.");
        }

        // Kiểm tra nếu đã đăng ký shop (Ngăn người dùng cố gắng truy cập form lần 2)
        if (storeService.hasShop(userId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã đăng ký Shop rồi.");
            return "redirect:/user/profile"; 
        }

        model.addAttribute("shopRegistrationDTO", new ShopRegistrationDTO());
        // Có thể cần thêm categories cho layout
        return "shop/register_form"; // Tạo file register_form.html
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public String processRegistration(@Valid @ModelAttribute("shopRegistrationDTO") ShopRegistrationDTO dto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes,
                                    Authentication auth,
                                    HttpSession session) {
        
        Integer userId = getCurrentUserId(auth.getName());
        if (userId == null) {
             return "redirect:/login";
        }

        if (result.hasErrors()) {
            return "shop/register_form"; 
        }
        
        try {
            storeService.registerNewShop(userId, dto);
            
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký Shop thành công! Vui lòng chờ xét duyệt.");
            
            // Xóa biến cờ hasRegisteredShop khỏi session để cập nhật Navbar
            session.removeAttribute("hasRegisteredShop");
            
            return "redirect:/user/profile"; 
            
        } catch (IllegalStateException e) {
            // Xử lý lỗi trùng tên Shop hoặc đã đăng ký rồi
            result.rejectValue("shopName", "shopName.exists", e.getMessage());
            return "shop/register_form";
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống khi đăng ký: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi đăng ký: " + e.getMessage());
            return "redirect:/shop/register";
        }
    }
}