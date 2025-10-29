package com.alotra.service.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
public class PasswordResetTokenService {

    @Value("${alotra.app.jwtSecret}")
    private String secretKey;

    /**
     * Tạo token theo cấu trúc: emailBase64.expiryTimestamp.hmacSignature
     * 
     * @param email Email người dùng
     * @param validityMinutes Thời gian hiệu lực (phút)
     * @return Token string
     */
    public String generateToken(String email, int validityMinutes) {
        // 1. Encode email thành Base64 URL-safe
        String emailB64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(email.getBytes(StandardCharsets.UTF_8));

        // 2. Tính expiry timestamp (giây)
        long expiryTimestamp = Instant.now().getEpochSecond() + (validityMinutes * 60L);
        String expStr = String.valueOf(expiryTimestamp);

        // 3. Tạo HMAC signature
        String dataToSign = emailB64 + "." + expStr;
        String signature = hmac(dataToSign);

        // 4. Ghép token: emailB64.expStr.signature
        return emailB64 + "." + expStr + "." + signature;
    }

    /**
     * Validate token và trích xuất email
     * 
     * @param token Token cần validate
     * @return Email nếu token hợp lệ, null nếu không hợp lệ
     */
    public String validateAndExtractEmail(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        // 1. Tách token thành 3 phần
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String emailB64 = parts[0];
        String expStr = parts[1];
        String providedSig = parts[2];

        // 2. Kiểm tra expiry
        long now = Instant.now().getEpochSecond();
        long exp;
        try {
            exp = Long.parseLong(expStr);
        } catch (NumberFormatException e) {
            return null;
        }

        if (exp < now) {
            // Token đã hết hạn
            return null;
        }

        // 3. Verify signature
        String dataToSign = emailB64 + "." + expStr;
        String expectedSig = hmac(dataToSign);

        if (!constantTimeEquals(providedSig, expectedSig)) {
            // Signature không khớp - có thể bị giả mạo
            return null;
        }

        // 4. Decode email
        try {
            byte[] emailBytes = Base64.getUrlDecoder().decode(emailB64);
            return new String(emailBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Tạo HMAC-SHA256 signature
     */
    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), 
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating HMAC", e);
        }
    }

    /**
     * So sánh 2 chuỗi theo thời gian cố định (chống timing attack)
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}