package com.alotra.controller.auth;

import org.springframework.security.core.Authentication;
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
        	return "redirect:/vendor/dashboard";
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

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() 
                && !authentication.getPrincipal().equals("anonymousUser")) {
        	return "redirect:/vendor/dashboard";
        }
        return "redirect:/login";
    }
    
    @GetMapping("/auth/403")
    public String accessDenied() {
        return "auth/403"; // Trang lỗi 403 - Access Denied
    }
    
//    private String redirectByRole(int roleId) {
//		switch (roleId) {
//			case 1: // Admin
//				return "redirect:/admin/dashboard";
//			case 2: // Customer
//				return "redirect:/customer/home";
//			case 3: // vendor
//				return "redirect:/vendor/dashboard";
//			default:
//				return "redirect:/login";
//		}
//	}
    
}