package com.alotra.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.repository.user.RoleRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.security.jwt.JwtTokenProvider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final String DEFAULT_ROLE = "CUSTOMER";
    private static final String FRONTEND_SUCCESS_URL = "http://localhost:3000/oauth2/success?token=";
    // hoặc "/" nếu bạn dùng Thymeleaf

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${alotra.app.jwtSecret}")
    private String jwtSecret;

    public OAuth2SuccessHandler(JwtTokenProvider jwtTokenProvider,
                                UserRepository userRepository,
                                RoleRepository roleRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        Object principal = authentication.getPrincipal();
        String jwtToken;

        if (principal instanceof DefaultOAuth2User oauth2User) {
            // Lấy thông tin từ Google
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String avatar = oauth2User.getAttribute("picture");

            // ✅ Tìm hoặc tạo user
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> createNewOAuth2User(email, name, avatar));

            // ✅ Tạo JWT token
            jwtToken = generateOAuth2Token(user);

            log.info("OAuth2 login success for {} (roles: {})",
                    email,
                    user.getRoles().stream()
                            .map(Role::getRoleName)
                            .collect(Collectors.joining(", ")));
        } else {
            throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
        }

        

      
        Cookie jwtCookie = new Cookie("jwtToken", jwtToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge((int) (jwtTokenProvider.getJwtExpirationMs() / 1000));
        jwtCookie.setSecure(false);
        response.addCookie(jwtCookie);
        response.sendRedirect("/");
     
    }
    
  
    private User createNewOAuth2User(String email, String name, String avatar) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(name != null ? name : "Người dùng Google");
        

        user.setUsername(email);
        user.setAvatarURL(avatar);
        user.setStatus((byte) 1); // Active
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        String randomPassword = UUID.randomUUID().toString();
   
        user.setPassword(new BCryptPasswordEncoder().encode(randomPassword));

        // Gán role mặc định
        Role defaultRole = roleRepository.findByRoleName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default role CUSTOMER not found"));

        user.setRoles(Set.of(defaultRole));

        User saved = userRepository.save(user);
        log.info("Tạo user mới từ Google: {} (ID={})", email, saved.getId());
        return saved;
    }

    private String generateOAuth2Token(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("roles", roles)
                .claim("userId", user.getId())
                .claim("fullName", user.getFullName())
                .claim("avatar", user.getAvatarURL())
                .claim("authProvider", "GOOGLE")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtTokenProvider.getJwtExpirationMs()))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)), SignatureAlgorithm.HS512)
                .compact();
    }
}
