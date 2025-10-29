package com.alotra.controller;

import com.alotra.entity.order.Order;
import com.alotra.repository.order.OrderRepository;
import com.alotra.service.cart.CartService;
import com.alotra.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class PaymentCallbackController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private CartService cartService;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * DÀNH CHO USER: Trang VNPay trả về sau khi người dùng thanh toán.
     */
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request) {
        String responseCode = request.getParameter("vnp_ResponseCode");
        String orderId = request.getParameter("vnp_TxnRef");

        if ("00".equals(responseCode)) {
            println("INFO", "User payment success - redirect to success page. OrderId: " + orderId);
            return "redirect:/order-success?orderId=" + orderId;
        } else {
            println("WARN", "User payment failed. ResponseCode: " + responseCode + ", OrderId: " + orderId);
            return "redirect:/order-failed?code=" + responseCode;
        }
    }

    /**
     * DÀNH CHO VNPAY SERVER: IPN - Xác nhận thanh toán
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) {
        println("INFO", "Received VNPay IPN callback");

        try {
            Map<String, String> params = extractParams(request);
            if (params.isEmpty()) {
                return errorResponse("99", "Empty parameters");
            }

            String vnp_SecureHash = params.remove("vnp_SecureHash");
            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                return errorResponse("97", "Missing vnp_SecureHash");
            }

            // === 1. XÁC THỰC CHỮ KÝ ===
            if (!verifySignature(params, vnp_SecureHash)) {
                println("WARN", "Invalid checksum from VNPay. IP: " + getClientIp(request));
                return errorResponse("97", "Invalid Checksum");
            }

            // === 2. LẤY DỮ LIỆU ===
            String txnRef = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String amountStr = params.get("vnp_Amount");
            String transactionNo = params.get("vnp_TransactionNo");

            if (isEmpty(txnRef)) {
                return errorResponse("01", "Missing vnp_TxnRef");
            }
            if (isEmpty(responseCode) || isEmpty(amountStr)) {
                return errorResponse("99", "Missing required params");
            }

            // === 3. PARSE ===
            Integer orderId;
            long vnpAmount;
            try {
                orderId = Integer.valueOf(txnRef);
                vnpAmount = Long.parseLong(amountStr) / 100;
            } catch (NumberFormatException e) {
                println("ERROR", "Invalid number format - TxnRef: " + txnRef + ", Amount: " + amountStr);
                e.printStackTrace();
                return errorResponse("99", "Invalid number format");
            }

            // === 4. TÌM ĐƠN HÀNG ===
            Order order;
            try {
                Optional<Order> orderOpt = orderRepository.findById(orderId);
                if (orderOpt.isEmpty()) {
                    println("WARN", "Order not found for IPN. OrderId: " + orderId);
                    return errorResponse("01", "Order not found");
                }
                order = orderOpt.get();
            } catch (Exception e) {
                println("ERROR", "Database error while fetching order ID: " + orderId);
                e.printStackTrace();
                return errorResponse("99", "System error");
            }

            // === 5. KIỂM TRA SỐ TIỀN ===
            if (order.getGrandTotal() == null || order.getGrandTotal().longValue() != vnpAmount) {
                println("WARN", "Amount mismatch. Expected: " + order.getGrandTotal() + ", Received: " + vnpAmount + ", OrderId: " + orderId);
                return errorResponse("04", "Invalid Amount");
            }

            // === 6. TRẠNG THÁI ===
            if (!"Unpaid".equals(order.getPaymentStatus())) {
                println("INFO", "Order already processed. Current status: " + order.getPaymentStatus() + ", OrderId: " + orderId);
                return successResponse();
            }

            // === 7. CẬP NHẬT ===
            if ("00".equals(responseCode)) {
                try {
                    order.setPaymentStatus("Paid");
                    order.setOrderStatus("Processing");
                    order.setPaidAt(LocalDateTime.now());
                    order.setTransactionID(transactionNo);

                    orderRepository.save(order);
                    println("INFO", "Order payment confirmed successfully. OrderId: " + orderId + ", TransactionNo: " + transactionNo);

                    clearCartSafely(order);

                } catch (Exception e) {
                    println("ERROR", "Failed to update order after payment. OrderId: " + orderId);
                    e.printStackTrace();
                    return errorResponse("99", "Failed to update order");
                }
            } else {
                order.setPaymentStatus("Failed");
                orderRepository.save(order);
                println("WARN", "Payment failed from VNPay. ResponseCode: " + responseCode + ", OrderId: " + orderId);
            }

            return successResponse();

        } catch (Exception e) {
            println("ERROR", "Unexpected error in VNPay IPN");
            e.printStackTrace();
            return errorResponse("99", "System error");
        }
    }

    // === HELPER METHODS ===

    private Map<String, String> extractParams(HttpServletRequest request) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String fieldName = paramNames.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                params.put(fieldName, URLDecoder.decode(fieldValue, StandardCharsets.UTF_8.name()));
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

    private void clearCartSafely(Order order) {
        if (cartService != null && order.getUser() != null) {
            try {
                cartService.clearCart(order.getUser());
                println("INFO", "Cart cleared for user: " + order.getUser().getId());
            } catch (Exception e) {
                println("ERROR", "Failed to clear cart for user: " + order.getUser().getId());
                e.printStackTrace();
            }
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
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

    // === HÀM IN LOG RA CONSOLE ===
    private void println(String level, String message) {
        String timestamp = LocalDateTime.now().format(LOG_TIME);
        System.out.println(timestamp + " " + level + " --- " + message);
    }
}