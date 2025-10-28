// File: VNPayUtil.java
package com.alotra.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

public class VNPayUtil {
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) throw new NullPointerException();
            Mac hmacSha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSha512.init(secretKey);
            byte[] bytes = hmacSha512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error hashing data with HmacSHA512", ex);
        }
    }

    public static String getIpAddress(HttpServletRequest request) {
        try {
            String ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) ipAdress = request.getRemoteAddr();
            return ipAdress;
        } catch (Exception e) {
            return "Invalid IP:" + e.getMessage();
        }
    }
}
