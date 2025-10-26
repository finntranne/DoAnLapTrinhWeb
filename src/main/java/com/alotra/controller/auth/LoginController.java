package com.alotra.controller.auth;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginPage(Authentication authentication) {
        // Nếu user đã đăng nhập thì redirect tới dashboard
        if (authentication != null && authentication.isAuthenticated() 
                && !authentication.getPrincipal().equals("anonymousUser")) {
        	String redirectUrl = determineTargetUrl(authentication);
        	return "redirect:" + redirectUrl;
        }
        return "auth/login";
    }

//    @GetMapping("/dashboard")
//    public String dashboard(Authentication authentication, Model model) {
//        // Kiểm tra xem user đã đăng nhập chưa
//        if (authentication == null || !authentication.isAuthenticated() 
//                || authentication.getPrincipal().equals("anonymousUser")) {
//            return "redirect:/login";
//        }
//        
//        // Thêm thông tin user vào model
//        model.addAttribute("username", authentication.getName());
//        model.addAttribute("authorities", authentication.getAuthorities());
//        
//        return "dashboard";
//    }
    
    @GetMapping("/auth/403")
    public String accessDenied() {
        return "auth/403"; // Trang lỗi 403 - Access Denied
    }
    
 // *** THÊM HÀM TRỢ GIÚP (Copy từ AuthController) ***
    private String determineTargetUrl(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            return "/admin/dashboard"; // Admin
        }
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("VENDOR"))) {
            return "/vendor/dashboard"; // Vendor
        }
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("CUSTOMER"))) {
            return "/"; // Customer về trang chủ
        }
        
        // Fallback
        return "/";
    }
    
}