package com.alotra.service.order.payment.sdk;

import com.alotra.config.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * VNPaySDK - SDK wrapper cho VNPay Payment Gateway
 * Chứa toàn bộ logic xử lý VNPay:
 * - Tạo URL thanh toán
 * - Xác minh callback signature
 * - Quản lý IP address
 * - Hash verification (HMAC-SHA512)
 * 
 * Tuân theo Adapter Pattern - chuyển đổi VNPayConfig thành PaymentStrategy interface
 */
public class VNPaySDK {
    
    private final VNPayConfig config;
    private static final String VNP_API_VERSION = "2.1.0";
    
    /**
     * Constructor - inject VNPayConfig từ Spring
     */
    public VNPaySDK(VNPayConfig config) {
        this.config = config;
    }
    
    /**
     * Tạo URL thanh toán VNPay
     * @param orderId ID đơn hàng
     * @param amount Số tiền (VND)
     * @param orderInfo Mô tả đơn hàng
     * @param ipAddress IP của khách hàng
     * @return URL thanh toán VNPay đầy đủ
     */
    public String createPaymentUrl(String orderId, long amount, String orderInfo, String ipAddress)
            throws UnsupportedEncodingException {

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put(VNPayConfig.VNP_VERSION, VNP_API_VERSION);
        vnpParams.put(VNPayConfig.VNP_COMMAND, "pay");
        vnpParams.put(VNPayConfig.VNP_TMNCODE, config.getTmnCode());
        vnpParams.put(VNPayConfig.VNP_AMOUNT, String.valueOf(amount * 100)); // VNPay yêu cầu amount x 100
        vnpParams.put(VNPayConfig.VNP_CURR_CODE, "VND");
        vnpParams.put(VNPayConfig.VNP_TXNREF, orderId);
        vnpParams.put(VNPayConfig.VNP_ORDER_INFO, orderInfo);
        vnpParams.put(VNPayConfig.VNP_ORDER_TYPE, "other");
        vnpParams.put(VNPayConfig.VNP_LOCALE, config.getLocale() != null ? config.getLocale() : "vn");
        vnpParams.put(VNPayConfig.VNP_RETURN_URL, config.getReturnUrl());
        vnpParams.put(VNPayConfig.VNP_IPADDR, ipAddress);

        // Đặt thời gian tạo và hết hạn
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put(VNPayConfig.VNP_CREATE_DATE, vnpCreateDate);

        cld.add(Calendar.MINUTE, 15); // Thời gian hết hạn (15 phút)
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put(VNPayConfig.VNP_EXPIRE_DATE, vnpExpireDate);

        // Tạo chữ ký
        String vnpSecureHash = hashAllFields(vnpParams, config.getHashSecret());
        vnpParams.put(VNPayConfig.VNP_SECURE_HASH, vnpSecureHash);

        // Xây dựng URL cuối cùng
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                query.append(java.net.URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(java.net.URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                }
            }
        }
        return config.getApiUrl() + "?" + query.toString();
    }

    /**
     * Lấy IP address từ HttpServletRequest
     * @param request HttpServletRequest
     * @return IP address
     */
    public String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            // Xử lý trường hợp có nhiều IP (chỉ lấy IP đầu tiên)
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
        } catch (Exception e) {
            ipAddress = "Invalid IP:" + e.getMessage();
        }
        // Trường hợp test local
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "127.0.0.1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
    }

    /**
     * Xác minh hash từ callback VNPay
     * @param vnpParams Parameters từ VNPay callback
     * @param vnpSecureHash Hash từ VNPay callback
     * @return true nếu hash hợp lệ, false nếu không
     */
    public boolean validatePaymentHash(Map<String, String> vnpParams, String vnpSecureHash) {
        try {
            String calculatedHash = hashAllFields(vnpParams, config.getHashSecret());
            return calculatedHash.equals(vnpSecureHash);
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error validating VNPAY hash: " + e.getMessage());
            return false;
        }
    }

    /**
     * Kiểm tra response code từ VNPay
     * @param responseCode Response code từ VNPay
     * @return true nếu thành công (code = "00"), false nếu không
     */
    public boolean isPaymentSuccess(String responseCode) {
        return "00".equals(responseCode);
    }

    /**
     * Hỗ trợ tạo chữ ký HMAC-SHA512
     */
    private static String hashAllFields(Map<String, String> fields, String secret)
            throws UnsupportedEncodingException {

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(java.net.URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        return hmacSHA512(secret, hashData.toString());
    }

    /**
     * Thuật toán HmacSHA512
     */
    private static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("Key or data is null");
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] resultBytes = hmac512.doFinal(dataBytes);

            StringBuilder sb = new StringBuilder(2 * resultBytes.length);
            for (byte b : resultBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error hashing data with HmacSHA512", e);
        }
    }
}
