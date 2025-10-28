//package com.alotra.controller;
//
//import com.alotra.entity.order.Order;
//import com.alotra.repository.order.OrderRepository;
//import com.alotra.service.cart.CartService;
//import com.alotra.util.VNPayUtil;
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//
//import java.io.UnsupportedEncodingException;
//import java.net.URLDecoder;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.*;
//
//@Controller
//public class PaymentCallbackController {
//
//    @Autowired
//    private OrderRepository orderRepository;
//
//    @Autowired
//    private CartService cartService;
//
//    @Value("${vnpay.hashSecret}")
//    private String hashSecret;
//
//    /**
//     * DÀNH CHO USER: Trang VNPay trả về sau khi người dùng thanh toán xong.
//     * (Client browser redirect)
//     */
//    @GetMapping("/vnpay-return")
//    public String vnpayReturn(HttpServletRequest request) {
//        String responseCode = request.getParameter("vnp_ResponseCode");
//        if ("00".equals(responseCode)) {
//            return "redirect:/order-success";
//        } else {
//            return "redirect:/order-failed";
//        }
//    }
//
//    /**
//     * DÀNH CHO VNPay SERVER: gọi ngầm (IPN callback) để xác nhận giao dịch.
//     * => Đây là nơi CẬP NHẬT TRẠNG THÁI thanh toán trong CSDL.
//     */
//    @GetMapping("/vnpay-ipn")
//    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) throws UnsupportedEncodingException {
//        // --- 1️⃣ Lấy toàn bộ tham số từ request ---
//        Map<String, String> params = new HashMap<>();
//        Enumeration<String> paramNames = request.getParameterNames();
//        while (paramNames.hasMoreElements()) {
//            String fieldName = paramNames.nextElement();
//            String fieldValue = URLDecoder.decode(
//                    request.getParameter(fieldName),
//                    StandardCharsets.UTF_8.toString()
//            );
//            params.put(fieldName, fieldValue);
//        }
//
//        // --- 2️⃣ Lấy và loại bỏ vnp_SecureHash ---
//        String vnp_SecureHash = params.remove("vnp_SecureHash");
//        String vnp_SecureHashType = params.remove("vnp_SecureHashType"); // Có thể có, không bắt buộc
//
//        // --- 3️⃣ Tạo chuỗi hashData (theo tài liệu VNPay) ---
//        List<String> fieldNames = new ArrayList<>(params.keySet());
//        Collections.sort(fieldNames);
//        StringBuilder hashData = new StringBuilder();
//
//        for (Iterator<String> it = fieldNames.iterator(); it.hasNext(); ) {
//            String fieldName = it.next();
//            String fieldValue = params.get(fieldName);
//            if (fieldValue != null && !fieldValue.isEmpty()) {
//                hashData.append(fieldName).append('=').append(fieldValue);
//                if (it.hasNext()) hashData.append('&');
//            }
//        }
//
//        // --- 4️⃣ Kiểm tra chữ ký ---
//        String calculatedHash = VNPayUtil.hmacSHA512(hashSecret, hashData.toString());
//        if (!calculatedHash.equals(vnp_SecureHash)) {
//            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}");
//        }
//
//        // --- 5️⃣ Kiểm tra logic nghiệp vụ ---
//        try {
//            Integer orderId = Integer.parseInt(params.get("vnp_TxnRef"));
//            String responseCode = params.get("vnp_ResponseCode");
//            long vnpAmount = Long.parseLong(params.get("vnp_Amount")) / 100; // VNPay gửi *100
//
//            Optional<Order> orderOpt = orderRepository.findById(orderId);
//            if (orderOpt.isEmpty()) {
//                return ResponseEntity.ok("{\"RspCode\":\"01\",\"Message\":\"Order not found\"}");
//            }
//
//            Order order = orderOpt.get();
//
//            // --- 6️⃣ Kiểm tra số tiền ---
//            if (order.getGrandTotal().longValue() != vnpAmount) {
//                return ResponseEntity.ok("{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}");
//            }
//
//            // --- 7️⃣ Kiểm tra trạng thái đã xử lý chưa ---
//            if (!"Unpaid".equalsIgnoreCase(order.getPaymentStatus())) {
//                return ResponseEntity.ok("{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}");
//            }
//
//            // --- 8️⃣ Cập nhật trạng thái ---
//            if ("00".equals(responseCode)) {
//                // ✅ Thanh toán thành công
//                order.setPaymentStatus("Paid");
//                order.setOrderStatus("Processing");
//                order.setPaidAt(LocalDateTime.now());
//                order.setTransactionID(params.get("vnp_TransactionNo")); // Mã giao dịch VNPay
//                cartService.clearCart(order.getUser()); // Xóa giỏ hàng người dùng
//            } else {
//                // ❌ Thanh toán thất bại
//                order.setPaymentStatus("Unpaid");
//                order.setOrderStatus("Failed");
//            }
//
//            orderRepository.save(order);
//            return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.ok("{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}");
//        }
//    }
//}
