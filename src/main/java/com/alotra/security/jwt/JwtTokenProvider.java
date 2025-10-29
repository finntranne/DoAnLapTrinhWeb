package com.alotra.security.jwt;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.alotra.security.MyUserDetails; // Import MyUserDetails
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider { // Đổi tên từ JwtUtils nếu cần

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${alotra.app.jwtSecret}")
    private String jwtSecret;

    @Value("${alotra.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    public int getJwtExpirationMs() {
        return jwtExpirationMs;
    }
    
    // Lấy Key bí mật từ chuỗi Base64
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. Tạo Token
    public String generateJwtToken(Authentication authentication) {
        
        // Lấy MyUserDetails từ đối tượng Authentication
        MyUserDetails userPrincipal = (MyUserDetails) authentication.getPrincipal();

        // Lấy danh sách roles (ví dụ: "VENDOR", "CUSTOMER")
        List<String> roles = userPrincipal.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(userPrincipal.getUsername()) // Dùng email làm subject
                .claim("roles", roles) // Thêm roles vào token
                .claim("userId", userPrincipal.getUser().getId()) // Thêm User ID
                .claim("shopId", userPrincipal.getShopId()) // Thêm Shop ID (sẽ là null nếu là customer)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // 2. Lấy Username (Email) từ Token
    public String getUsernameFromJwtToken(String token) {
        Claims claims = Jwts.parser()
                            .verifyWith(getSigningKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
        return claims.getSubject();
    }

    // 3. Xác thực Token
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
             logger.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }
    
 // *** THÊM HÀM MỚI NÀY ***
    /**
     * Tạo một token xác thực mục đích đơn (ví dụ: kích hoạt tài khoản, reset mật khẩu)
     * @param subject (thường là email)
     * @param purpose (ví dụ: "REGISTER" hoặc "RESET_PASSWORD")
     * @param expirationMs (thời gian hết hạn, ví dụ: 900000ms = 15 phút)
     * @return Chuỗi JWT
     */
    public String generateVerificationToken(String subject, String purpose, int expirationMs) {
        return Jwts.builder()
                .subject(subject)
                .claim("purpose", purpose) // Thêm mục đích vào token
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // *** THÊM HÀM NÀY ĐỂ LẤY CLAIMS (BAO GỒM CẢ PURPOSE) ***
    public Claims getClaimsFromJwtToken(String token) {
        return Jwts.parser()
                   .verifyWith(getSigningKey())
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }
   
}



