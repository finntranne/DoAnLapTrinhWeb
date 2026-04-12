package com.alotra.service.order;

import com.alotra.entity.order.Order;
import com.alotra.entity.order.Payment;
import com.alotra.enums.PaymentStatus;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.PaymentRepository;
import com.alotra.util.OrderPricingUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * PaymentCallbackService - Xử lý callback từ các payment gateway
 * Tuân theo Service Layer Pattern
 * 
 * Chức năng:
 * - Validate signature từ VNPay callback
 * - Verify order & payment information
 * - Update payment status trong database
 * - Xử lý IPN (Instant Payment Notification) từ VNPay server
 */
@Service
public class PaymentCallbackService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Value("${vnpay.hash-secret:VZ4WXT2UEBZRYKHP2TI9CW5V4HNSNGVO}")
    private String hashSecret;
    
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String RESPONSE_CODE_SUCCESS = "00";
    
    /**
     * Xử lý VNPay IPN callback
     * Server-to-server notification từ VNPay
     * @param request HTTP request từ VNPay
     * @return Map chứa status và message
     */
    public Map<String, String> handleVNPayIpnCallback(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // 1. Extract parameters
            Map<String, String> params = extractParameters(request);
            if (params.isEmpty()) {
                return errorResponse("99", "Empty parameters");
            }
            
            // 2. Verify signature
            String secureHash = params.remove("vnp_SecureHash");
            if (secureHash == null || secureHash.isEmpty()) {
                return errorResponse("97", "Missing vnp_SecureHash");
            }
            
            if (!verifySignature(params, secureHash)) {
                logCallback("WARN", "Invalid checksum from VNPay. IP: " + getClientIp(request));
                return errorResponse("97", "Invalid Checksum");
            }
            
            // 3. Extract transaction info
            String txnRef = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String amountStr = params.get("vnp_Amount");
            String transactionNo = params.get("vnp_TransactionNo");
            
            if (isEmpty(txnRef) || isEmpty(responseCode) || isEmpty(amountStr)) {
                return errorResponse("99", "Missing required params");
            }
            
            // 4. Parse orderId and amount
            Integer orderId;
            long vnpAmount;
            try {
                orderId = Integer.valueOf(txnRef);
                vnpAmount = Long.parseLong(amountStr) / 100;
            } catch (NumberFormatException e) {
                logCallback("ERROR", "Invalid number format - TxnRef: " + txnRef + ", Amount: " + amountStr);
                return errorResponse("99", "Invalid number format");
            }
            
            // 5. Verify order exists
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                logCallback("WARN", "Order not found for IPN. OrderId: " + orderId);
                return errorResponse("01", "Order not found");
            }
            Order order = orderOpt.get();
            
            // 6. Verify payment exists
            Optional<Payment> paymentOpt = paymentRepository.findByOrder_OrderID(orderId);
            if (paymentOpt.isEmpty()) {
                logCallback("WARN", "Payment not found for order. OrderId: " + orderId);
                return errorResponse("01", "Payment not found");
            }
            Payment payment = paymentOpt.get();
            
            // 7. Verify amount
            long expectedAmount = OrderPricingUtils.calculateOrderTotal(order).longValue();
            if (expectedAmount != vnpAmount) {
                logCallback("WARN", "Amount mismatch. Expected: " + expectedAmount + 
                           ", Received: " + vnpAmount + ", OrderId: " + orderId);
                return errorResponse("04", "Invalid Amount");
            }
            
            // 8. Check if payment already processed
            if (payment.getStatus() == PaymentStatus.PAID) {
                logCallback("INFO", "Order already paid. OrderId: " + orderId);
                return successResponse();
            }
            
            // 9. Update payment based on response code
            if (RESPONSE_CODE_SUCCESS.equals(responseCode)) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                payment.setTransactionCode(transactionNo);
                paymentRepository.save(payment);
                
                // Update order status
                order.setOrderStatus("Confirmed");
                orderRepository.save(order);
                
                logCallback("INFO", "Payment confirmed. OrderId: " + orderId + ", TxnNo: " + transactionNo);
            } else {
                payment.setStatus(PaymentStatus.UNPAID);
                payment.setTransactionCode(transactionNo);
                paymentRepository.save(payment);
                
                order.setOrderStatus("PaymentFailed");
                orderRepository.save(order);
                
                logCallback("WARN", "Payment failed at gateway. OrderId: " + orderId + 
                           ", ResponseCode: " + responseCode);
            }
            
            return successResponse();
            
        } catch (Exception e) {
            logCallback("ERROR", "Unexpected error in VNPay IPN: " + e.getMessage());
            e.printStackTrace();
            return errorResponse("99", "System error");
        }
    }
    
    /**
     * Xử lý user redirect return từ VNPay
     * Client-side redirect (browser) từ VNPay payment page
     * @param request HTTP request từ VNPay
     * @return Map chứa status và orderInfo
     */
    public Map<String, Object> handleVNPayUserReturn(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String responseCode = request.getParameter("vnp_ResponseCode");
            String orderId = request.getParameter("vnp_TxnRef");
            
            if (RESPONSE_CODE_SUCCESS.equals(responseCode)) {
                logCallback("INFO", "VNPay return - Payment success. OrderId: " + orderId);
                response.put("success", true);
                response.put("message", "Thanh toán thành công");
                response.put("orderId", orderId);
                response.put("redirectUrl", "/order-success?orderId=" + orderId);
            } else {
                logCallback("WARN", "VNPay return - Payment failed. ResponseCode: " + responseCode + 
                           ", OrderId: " + orderId);
                response.put("success", false);
                response.put("message", "Thanh toán thất bại");
                response.put("redirectUrl", "/order-failed?code=" + responseCode);
            }
            
            return response;
            
        } catch (Exception e) {
            logCallback("ERROR", "Error handling VNPay user return: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi xử lý callback: " + e.getMessage());
            return errorResponse;
        }
    }
    
    /**
     * Verify VNPay signature using HMAC-SHA512
     */
    private boolean verifySignature(Map<String, String> params, String secureHash) {
        try {
            // Sort parameters by key
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);
            
            // Build hash data
            StringBuilder hashData = new StringBuilder();
            for (String field : fieldNames) {
                String value = params.get(field);
                if (value != null && !value.isEmpty()) {
                    hashData.append(field).append('=').append(value).append('&');
                }
            }
            
            if (hashData.length() > 0) {
                hashData.deleteCharAt(hashData.length() - 1);
            }
            
            // Calculate hash
            String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
            boolean isValid = calculatedHash.equals(secureHash);
            
            if (!isValid) {
                logCallback("DEBUG", "Signature mismatch. Expected: " + calculatedHash + 
                           ", Received: " + secureHash);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logCallback("ERROR", "Error verifying signature: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Calculate HMAC-SHA512
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec spec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(spec);
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }
    
    /**
     * Extract parameters từ HTTP request
     */
    private Map<String, String> extractParameters(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String fieldName = paramNames.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    params.put(fieldName, java.net.URLDecoder.decode(fieldValue, StandardCharsets.UTF_8));
                } catch (Exception e) {
                    params.put(fieldName, fieldValue);
                }
            }
        }
        
        return params;
    }
    
    // Helper methods
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private void logCallback(String level, String message) {
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now());
        System.out.println("[" + timestamp + "] [" + level + "] [PaymentCallback] " + message);
    }
    
    private Map<String, String> successResponse() {
        Map<String, String> response = new HashMap<>();
        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return response;
    }
    
    private Map<String, String> errorResponse(String code, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("RspCode", code);
        response.put("Message", message);
        return response;
    }
}
