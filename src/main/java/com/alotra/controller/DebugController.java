package com.alotra.controller;

import com.alotra.entity.user.User;
import com.alotra.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /api/debug/user
     * Lấy thông tin user hiện tại đã authenticated
     */
    @GetMapping("/user")
    public String getCurrentUserInfo(Authentication authentication) {
        logger.info("=== DEBUG: Current User Info ===");

        if (authentication == null) {
            String msg = "❌ Not authenticated";
            logger.warn(msg);
            return msg;
        }

        String username = authentication.getName();
        boolean isAuthenticated = authentication.isAuthenticated();

        logger.info("Username: {}", username);
        logger.info("Is Authenticated: {}", isAuthenticated);

        // Lấy authorities
        java.util.Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        logger.info("Number of authorities: {}", authorities.size());

        String authoritiesStr = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        logger.info("Authorities: {}", authoritiesStr);

        // Chi tiết authorities
        StringBuilder sb = new StringBuilder();
        sb.append("✅ CURRENT USER INFO\n");
        sb.append("====================\n");
        sb.append("Username: ").append(username).append("\n");
        sb.append("Is Authenticated: ").append(isAuthenticated).append("\n");
        sb.append("Number of Authorities: ").append(authorities.size()).append("\n");
        sb.append("Authorities List:\n");

        authorities.forEach(auth -> {
            sb.append("  - ").append(auth.getAuthority()).append("\n");
            logger.info("  Authority: {}", auth.getAuthority());
        });

        String result = sb.toString();
        logger.info("Result:\n{}", result);
        return result;
    }
}