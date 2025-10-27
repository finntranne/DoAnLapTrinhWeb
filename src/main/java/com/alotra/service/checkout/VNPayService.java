package com.alotra.service.checkout;

import com.alotra.entity.order.Order;
import com.alotra.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String vnpayUrl;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    public String createPaymentUrl(Order order, HttpServletRequest request) throws UnsupportedEncodingException {
        
        // SỐ TIỀN: Lấy từ grandTotal (BigDecimal) và nhân 100
        long amount = order.getGrandTotal().multiply(new BigDecimal(100)).longValue();
        
        // MÃ ĐƠN HÀNG: Dùng OrderID (đã đổi thành Integer)
        String vnp_TxnRef = String.valueOf(order.getOrderID()); // ← SỬA TÊN METHOD
        
        String vnp_IpAddr = VNPayUtil.getIpAddress(request);
        String vnp_OrderInfo = "Thanh toan don hang ALOTRA ID: " + order.getOrderID(); // ← SỬA TÊN METHOD
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", tmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Thời gian tạo
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        
        // Thời gian hết hạn (15 phút)
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // --- TẠO CHỮ KÝ (GIỐNG CODE CỦ Y NGUYÊN) ---
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                
                // === SỬA TẠI ĐÂY: Dùng UTF-8 ===
                String fieldValueEncoded = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString());
                
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValueEncoded);
                hashData.append('&'); // Luôn thêm &
                
                // Build query 
                // === SỬA TẠI ĐÂY: Dùng UTF-8 ===
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                query.append('=');
                query.append(fieldValueEncoded);
                query.append('&'); // Luôn thêm &
            }
        }
        
        // Xóa dấu & cuối cùng
        hashData.deleteCharAt(hashData.length() - 1);
        query.deleteCharAt(query.length() - 1);
        
        String vnp_SecureHash = VNPayUtil.hmacSHA512(hashSecret, hashData.toString());
        query.append("&vnp_SecureHash=");
        query.append(vnp_SecureHash);
        
        String paymentUrl = vnpayUrl + "?" + query.toString();
        
        // Debug log
        System.out.println("=== VNPay Payment URL ===");
        System.out.println("Order ID: " + order.getOrderID());
        System.out.println("Amount: " + amount);
        System.out.println("Hash Data: " + hashData.toString());
        System.out.println("Secure Hash: " + vnp_SecureHash);
        System.out.println("Payment URL: " + paymentUrl);
        
        return paymentUrl;
    }
}