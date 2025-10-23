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
import java.time.Instant;
import java.util.*;

@Controller
public class PaymentCallbackController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private CartService cartService;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    /**
     * DÀNH CHO USER: Trang VNPay trả về sau khi người dùng thanh toán.
     * Chỉ dùng để hiển thị thông báo.
     */
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request) {
        String responseCode = request.getParameter("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            // Thanh toán thành công -> chuyển về trang success
            return "redirect:/order-success";
        } else {
            // Thanh toán thất bại -> chuyển về trang failed
            return "redirect:/order-failed";
        }
    }

    /**
     * DÀNH CHO VNPAY SERVER: VNPay gọi ngầm vào đây (IPN) để xác nhận.
     * Đây là nơi CẬP NHẬT CSDL.
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) throws UnsupportedEncodingException {

        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String fieldName = paramNames.nextElement();
            String fieldValue = URLDecoder.decode(request.getParameter(fieldName), StandardCharsets.US_ASCII.toString());
            params.put(fieldName, fieldValue);
        }

        String vnp_SecureHash = params.remove("vnp_SecureHash");

        // --- XÁC THỰC CHỮ KÝ ---
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);
                hashData.append('&');
            }
        }
        hashData.deleteCharAt(hashData.length() - 1);
        
        String calculatedHash = VNPayUtil.hmacSHA512(hashSecret, hashData.toString());

        if (!calculatedHash.equals(vnp_SecureHash)) {
            // RspCode 97: Chữ ký không hợp lệ
            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}");
        }

        // --- KIỂM TRA LOGIC NGHIỆP VỤ ---
        try {
            Integer orderId = Integer.parseInt(params.get("vnp_TxnRef"));
            String responseCode = params.get("vnp_ResponseCode");
            long vnpAmount = Long.parseLong(params.get("vnp_Amount")) / 100;

            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isEmpty()) {
                // RspCode 01: Đơn hàng không tồn tại
                return ResponseEntity.ok("{\"RspCode\":\"01\",\"Message\":\"Order not found\"}");
            }

            Order order = orderOpt.get();

            if (order.getGrandTotal().longValue() != vnpAmount) {
                 // RspCode 04: Số tiền không hợp lệ
                return ResponseEntity.ok("{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}");
            }
            
            if (!"Unpaid".equals(order.getPaymentStatus())) {
                 // RspCode 02: Đơn hàng đã được thanh toán
                return ResponseEntity.ok("{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}");
            }

            // --- CẬP NHẬT CSDL ---
            if ("00".equals(responseCode)) {
                // Thanh toán thành công
                order.setPaymentStatus("Paid"); // Trạng thái "Đã thanh toán"
                order.setOrderStatus("Processing"); // Chuyển sang "Đang xử lý"
                order.setPaidAt(Instant.now()); // Ghi lại thời gian
                
                // Xóa giỏ hàng
                cartService.clearCart(order.getCustomer());
            } else {
                // Thanh toán thất bại (Giữ 'Unpaid' hoặc chuyển 'Failed' tùy bạn)
                 order.setPaymentStatus("Unpaid"); 
            }
            
            orderRepository.save(order);

            // RspCode 00: Thành công
            return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");

        } catch (Exception e) {
            // RspCode 99: Lỗi không xác định
            return ResponseEntity.ok("{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}");
        }
    }  
}