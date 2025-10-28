package com.alotra.service.checkout;

import com.alotra.config.VNPayConfig;
import com.alotra.entity.order.Order;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class VNPayService {

    /**
     * Tạo URL thanh toán VNPay
     */
    public String createPaymentUrl(Order order, HttpServletRequest request) throws UnsupportedEncodingException {
        
        // Số tiền phải là số nguyên (đơn vị VNĐ)
        long amount = order.getGrandTotal().longValue(); 
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VNPayConfig.VNP_VERSION);
        vnp_Params.put("vnp_Command", VNPayConfig.VNP_COMMAND);
        vnp_Params.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // Số tiền x 100
        vnp_Params.put("vnp_CurrCode", VNPayConfig.VNP_CURRENCY_CODE);
        vnp_Params.put("vnp_Locale", VNPayConfig.VNP_LOCALE);
        vnp_Params.put("vnp_TxnRef", String.valueOf(order.getOrderID())); // Mã giao dịch (Mã đơn hàng)
        
        // Thêm các tham số bắt buộc khác
        String vnp_CreateDate = VNPayConfig.getVnpayDateFormat(new Date());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.put("vnp_IpAddr", getIpAddress(request));
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang: " + order.getOrderID());
        vnp_Params.put("vnp_OrderType", "other"); // Loại hàng hóa
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.VNP_RETURN_URL);
        vnp_Params.put("vnp_ExpireDate", VNPayConfig.getVnpayExpireDate()); // Thời gian hết hạn

        // 1. Sắp xếp các tham số
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        // 2. Tạo chuỗi truy vấn (Query String) và chuỗi Hash Data
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        // 3. Tạo Vnp_SecureHash (Checksum)
        String queryUrl = VNPayConfig.VNP_PAY_URL + "?" + query.toString();
        String secureHash = hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + secureHash;
        
        return queryUrl;
    }

    /**
     * Xử lý kết quả trả về từ VNPay (Checksum và Response Code)
     * Trả về "00" nếu Checksum hợp lệ, "97" nếu Checksum thất bại
     */
    public String paymentReturn(HttpServletRequest request) {
        Map<String, String> vnp_Params = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        
        while (params.hasMoreElements()) {
            String fieldName = (String) params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0) && !fieldName.startsWith("vnp_SecureHash")) {
                vnp_Params.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (vnp_SecureHash != null && vnp_SecureHash.length() > 0) {
            
            // 1. Sắp xếp và tạo chuỗi Hash Data từ các tham số VNPay gửi về
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = (String) itr.next();
                String fieldValue = (String) vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(fieldValue);
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
            
            // 2. Tạo Checksum của riêng mình và so sánh
            String secureHash = hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData.toString());
            
            if (secureHash.equals(vnp_SecureHash)) {
                return "00"; // Checksum hợp lệ
            } else {
                return "97"; // Checksum không hợp lệ
            }
        }
        return "97"; // Mặc định là lỗi Checksum
    }

    // =======================================================
    // Hàm tiện ích: Hashing và Lấy IP
    // =======================================================
    
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSHA512.init(secretKey);
            byte[] hash = hmacSHA512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo HmacSHA512: " + e.getMessage(), e);
        }
    }
    
    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}