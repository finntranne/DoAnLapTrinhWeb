package com.alotra.security.jwt;

import com.alotra.security.UserDetailsServiceImpl; // Import UserDetailsServiceImpl
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter { // Đổi tên nếu cần

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService; // Dùng UserDetailsServiceImpl

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Lấy token từ header
            String jwt = parseJwt(request);

            // 2. Validate token
            if (jwt != null && tokenProvider.validateJwtToken(jwt)) {
                
                // 3. Lấy username (email) từ token
                String username = tokenProvider.getUsernameFromJwtToken(jwt);

                // 4. Tải UserDetails
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // 5. Tạo đối tượng xác thực
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Set vào SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("User authenticated via JWT: {}", username);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        // Tiếp tục chuỗi filter
        filterChain.doFilter(request, response);
    }

    // Hàm trợ giúp lấy token từ "Authorization: Bearer <token>"
    private String parseJwt(HttpServletRequest request) {
        // 1. Ưu tiên đọc từ Header "Authorization" (cho API nếu có)
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        // 2. Nếu không có header, đọc từ Cookie (cho Web)
        Cookie jwtCookie = WebUtils.getCookie(request, "jwtToken"); // Dùng WebUtils
        if (jwtCookie != null) {
            return jwtCookie.getValue();
        }
        
        return null; // Không tìm thấy token
    }
}