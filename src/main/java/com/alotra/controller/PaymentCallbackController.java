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
import com.alotra.util.OrderPricingUtils;
import com.alotra.util.VNPayUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PaymentCallbackController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request) {
        String responseCode = request.getParameter("vnp_ResponseCode");
        String orderId = request.getParameter("vnp_TxnRef");

        if ("00".equals(responseCode)) {
            println("INFO", "User payment success - redirect to success page. OrderId: " + orderId);
            return "redirect:/order-success?orderId=" + orderId;
        }

        println("WARN", "User payment failed. ResponseCode: " + responseCode + ", OrderId: " + orderId);
        return "redirect:/order-failed?code=" + responseCode;
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) {
        println("INFO", "Received VNPay IPN callback");

        try {
            Map<String, String> params = extractParams(request);
            if (params.isEmpty()) {
                return errorResponse("99", "Empty parameters");
            }

            String secureHash = params.remove("vnp_SecureHash");
            if (secureHash == null || secureHash.isEmpty()) {
                return errorResponse("97", "Missing vnp_SecureHash");
            }

            if (!verifySignature(params, secureHash)) {
                println("WARN", "Invalid checksum from VNPay. IP: " + getClientIp(request));
                return errorResponse("97", "Invalid Checksum");
            }

            String txnRef = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String amountStr = params.get("vnp_Amount");
            String transactionNo = params.get("vnp_TransactionNo");

            if (isEmpty(txnRef) || isEmpty(responseCode) || isEmpty(amountStr)) {
                return errorResponse("99", "Missing required params");
            }

            Integer orderId;
            long vnpAmount;
            try {
                orderId = Integer.valueOf(txnRef);
                vnpAmount = Long.parseLong(amountStr) / 100;
            } catch (NumberFormatException e) {
                println("ERROR", "Invalid number format - TxnRef: " + txnRef + ", Amount: " + amountStr);
                return errorResponse("99", "Invalid number format");
            }

            Order order = orderRepository.findById(orderId)
                    .orElse(null);
            if (order == null) {
                println("WARN", "Order not found for IPN. OrderId: " + orderId);
                return errorResponse("01", "Order not found");
            }

            Payment payment = paymentRepository.findByOrder_OrderID(orderId).orElse(null);
            if (payment == null) {
                println("WARN", "Payment not found for order. OrderId: " + orderId);
                return errorResponse("01", "Payment not found");
            }

            long expectedAmount = OrderPricingUtils.calculateOrderTotal(order).longValue();
            if (expectedAmount != vnpAmount) {
                println("WARN", "Amount mismatch. Expected: " + expectedAmount + ", Received: " + vnpAmount
                        + ", OrderId: " + orderId);
                return errorResponse("04", "Invalid Amount");
            }

            if (payment.getStatus() == PaymentStatus.PAID) {
                println("INFO", "Order already processed. OrderId: " + orderId);
                return successResponse();
            }

            if ("00".equals(responseCode)) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                payment.setTransactionCode(transactionNo);
                paymentRepository.save(payment);

                order.setOrderStatus("Confirmed");
                orderRepository.save(order);
            } else {
                payment.setStatus(PaymentStatus.UNPAID);
                paymentRepository.save(payment);
                order.setOrderStatus("PaymentFailed");
                orderRepository.save(order);
            }

            return successResponse();
        } catch (Exception e) {
            println("ERROR", "Unexpected error in VNPay IPN: " + e.getMessage());
            return errorResponse("99", "System error");
        }
    }

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
        println("DEBUG", "IPN Params: " + params);
        return params;
    }

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
        return calculatedHash.equals(secureHash);
    }

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

    private ResponseEntity<String> successResponse() {
        return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
    }

    private ResponseEntity<String> errorResponse(String code, String message) {
        println("WARN", "IPN Error - Code: " + code + ", Message: " + message);
        return ResponseEntity.ok(String.format("{\"RspCode\":\"%s\",\"Message\":\"%s\"}", code, message));
    }

    private void println(String level, String message) {
        String timestamp = LocalDateTime.now().format(LOG_TIME);
        System.out.println(timestamp + " " + level + " --- " + message);
    }
}
