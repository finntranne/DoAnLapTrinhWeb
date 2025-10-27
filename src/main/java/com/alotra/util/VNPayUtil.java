package com.alotra.util; // Hoặc package config/util của bạn

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;

public class VNPayUtil {

    /**
     * Tạo chữ ký HmacSHA512
     */
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            Mac hmacSha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmacSha512.init(secretKey);
            byte[] bytes = hmacSha512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception ex) {
            throw new RuntimeException("Error hashing data with HmacSHA512", ex);
        }
    }

    /**
     * Lấy IP của client
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;
    }
    
    public class TestVNPayHash {
        public static void main(String[] args) {
            String hashSecret = "ZGXD52K58OCC4GQB2BDFQ0XO9JTLG2UM";
            String testData = "vnp_Amount=10000000&vnp_Command=pay&vnp_TmnCode=4NI19HF0&vnp_TxnRef=123";
            
            String hash = VNPayUtil.hmacSHA512(hashSecret, testData);
            
            System.out.println("Test Hash: " + hash);
            System.out.println("Hash length: " + hash.length());
        }
        
    }
    
}
