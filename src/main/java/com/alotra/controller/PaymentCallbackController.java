package com.alotra.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.alotra.entity.order.Order;
import com.alotra.entity.order.Payment;
import com.alotra.enums.PaymentStatus;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.PaymentRepository;
import com.alotra.service.order.PaymentService;
import com.alotra.util.OrderPricingUtils;
import com.alotra.util.VNPayUtil;

import jakarta.servlet.http.HttpServletRequest;

/**
 * PaymentCallbackController - Xử lý callback từ các gateway thanh toán
 * Áp dụng Design Pattern: Strategy, Adapter, Factory (qua PaymentService)
 */
@Controller
public class PaymentCallbackController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Value("${vnpay.hash-secret:VZ4WXT2UEBZRYKHP2TI9CW5V4HNSNGVO}")
    private String hashSecret;

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Xử lý user redirect back từ VNPay
     * @param request HTTP request từ client
     * @return redirect to success/failed page
     */
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request) {
        String responseCode = request.getParameter("vnp_ResponseCode");
        String orderId = request.getParameter("vnp_TxnRef");

        if ("00".equals(responseCode)) {
            printLog("INFO", "VNPay return - Payment success. OrderId: " + orderId);
            return "redirect:/order-success?orderId=" + orderId;
        }

        printLog("WARN", "VNPay return - Payment failed. ResponseCode: " + responseCode + ", OrderId: " + orderId);
        return "redirect:/order-failed?code=" + responseCode;
    }

    /**
     * Xử lý IPN callback từ VNPay server
     * IPN = Instant Payment Notification (notification từ payment gateway)
     * @param request HTTP request từ VNPay
     * @return JSON response
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) {
        printLog("INFO", "Received VNPay IPN callback");

        try {
            // 1. Extract parameters từ request
            Map<String, String> params = extractParams(request);
            if (params.isEmpty()) {
                return errorResponse("99", "Empty parameters");
            }

            // 2. Verify signature
            String secureHash = params.remove("vnp_SecureHash");
            if (secureHash == null || secureHash.isEmpty()) {
                return errorResponse("97", "Missing vnp_SecureHash");
            }

            if (!verifySignature(params, secureHash)) {
                printLog("WARN", "Invalid checksum from VNPay. IP: " + getClientIp(request));
                return errorResponse("97", "Invalid Checksum");
            }

            // 3. Extract transaction information
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
                printLog("ERROR", "Invalid number format - TxnRef: " + txnRef + ", Amount: " + amountStr);
                return errorResponse("99", "Invalid number format");
            }

            // 5. Verify order exists
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                printLog("WARN", "Order not found for IPN. OrderId: " + orderId);
                return errorResponse("01", "Order not found");
            }
            Order order = orderOpt.get();

            // 6. Verify payment exists
            Optional<Payment> paymentOpt = paymentRepository.findByOrder_OrderID(orderId);
            if (paymentOpt.isEmpty()) {
                printLog("WARN", "Payment not found for order. OrderId: " + orderId);
                return errorResponse("01", "Payment not found");
            }
            Payment payment = paymentOpt.get();

            // 7. Verify amount
            long expectedAmount = OrderPricingUtils.calculateOrderTotal(order).longValue();
            if (expectedAmount != vnpAmount) {
                printLog("WARN", "Amount mismatch. Expected: " + expectedAmount + ", Received: " + vnpAmount
                        + ", OrderId: " + orderId);
                return errorResponse("04", "Invalid Amount");
            }

            // 8. Check if payment already processed
            if (payment.getStatus() == PaymentStatus.PAID) {
                printLog("INFO", "Order already paid. OrderId: " + orderId);
                return successResponse();
            }

            // 9. Update payment based on response code
            if ("00".equals(responseCode)) {
                // Payment success
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                payment.setTransactionCode(transactionNo);
                paymentRepository.save(payment);
                
                // Update order status to Confirmed
                order.setOrderStatus("Confirmed");
                orderRepository.save(order);
                
                printLog("INFO", "Payment confirmed. OrderId: " + orderId + ", TxnNo: " + transactionNo);
            } else {
                // Payment failed
                payment.setStatus(PaymentStatus.UNPAID);
                payment.setTransactionCode(transactionNo);
                paymentRepository.save(payment);
                
                order.setOrderStatus("PaymentFailed");
                orderRepository.save(order);
                
                printLog("WARN", "Payment failed at gateway. OrderId: " + orderId + ", ResponseCode: " + responseCode);
            }

            return successResponse();
            
        } catch (Exception e) {
            printLog("ERROR", "Unexpected error in VNPay IPN: " + e.getMessage());
            e.printStackTrace();
            return errorResponse("99", "System error");
        }
    }

    /**
     * Extract parameters từ request
     */
    private Map<String, String> extractParams(HttpServletRequest request) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String fieldName = paramNames.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                params.put(fieldName, URLDecoder.decode(fieldValue, StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    /**
     * Verify HMAC SHA512 signature từ VNPay
     */
    private boolean verifySignature(Map<String, String> params, String secureHash) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

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

        String calculatedHash = VNPayUtil.hmacSHA512(hashSecret, hashData.toString());
        boolean isValid = calculatedHash.equals(secureHash);
        
        if (!isValid) {
            printLog("DEBUG", "Signature mismatch. Expected: " + calculatedHash + ", Received: " + secureHash);
        }
        
        return isValid;
    }

    /**
     * Helper methods
     */
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

    /**
     * Response helpers
     */
    private ResponseEntity<String> successResponse() {
        return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
    }

    private ResponseEntity<String> errorResponse(String code, String message) {
        printLog("WARN", "IPN Error - Code: " + code + ", Message: " + message);
        return ResponseEntity.ok(String.format("{\"RspCode\":\"%s\",\"Message\":\"%s\"}", code, message));
    }

    private void printLog(String level, String message) {
        String timestamp = LocalDateTime.now().format(LOG_TIME);
        System.out.println(timestamp + " [PaymentCallback] " + level + " --- " + message);
    }
}
