package com.alotra.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectController {

    @GetMapping("/redirect")
    public String redirectBasedOnRole(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        // Kiểm tra quyền
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER"))) {
            return "redirect:/user";
        } else {
            return "redirect:/access-denied";
        }
    }
}