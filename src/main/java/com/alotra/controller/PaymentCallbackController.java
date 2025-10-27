package com.alotra.controller;

import com.alotra.entity.order.Order;
import com.alotra.entity.user.User;
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
import java.time.LocalDateTime;
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

          if ("00".equals(responseCode)) {
              System.out.println("VNPay IPN Success for Order ID: " + orderId + ". Updating status...");
              // Thanh toán thành công
              order.setPaymentStatus("Paid");
              order.setOrderStatus("Confirmed"); // Chuyển sang Đã xác nhận (hoặc Processing tùy quy trình)
              order.setPaidAt(LocalDateTime.now()); // *** SỬA: Dùng LocalDateTime ***
              order.setTransactionID(params.get("vnp_TransactionNo")); // Lưu mã giao dịch VNPay

              // Lấy User từ Order để xóa giỏ hàng
              User user = order.getUser();
              if (user != null && cartService != null) { // Kiểm tra cartService không null
                  cartService.clearCart(user); // *** SỬA: Gọi clearCart với User ***
                  System.out.println("Cart cleared for User ID: " + user.getId());
              } else {
                   System.err.println("VNPay IPN Warning: Could not clear cart. User or CartService is null for Order ID " + orderId);
              }

          } else {
              // Thanh toán thất bại
               System.out.println("VNPay IPN Failed for Order ID: " + orderId + ". Status remains Unpaid.");
              // Giữ 'Unpaid' hoặc chuyển 'Failed' tùy logic
               order.setPaymentStatus("Unpaid"); // Hoặc "Failed"
               // Có thể set OrderStatus thành "Cancelled" nếu thất bại?
               // order.setOrderStatus("Cancelled");
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

//package com.alotra.controller; // Giữ package này
//
//// Import entity đã merge
//import com.alotra.entity.order.Order;
//import com.alotra.entity.user.User; // Sử dụng User
//
//// Import Repository và Service đã merge
//import com.alotra.repository.order.OrderRepository;
//import com.alotra.service.cart.CartService;
//import com.alotra.service.user.UserService; // Sử dụng UserService
//import com.alotra.util.VNPayUtil; // Giữ lại Util
//
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.transaction.annotation.Transactional; // *** THÊM Transactional ***
//
//import java.io.UnsupportedEncodingException;
//import java.math.BigDecimal;
//import java.net.URLDecoder;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime; // *** SỬA: Dùng LocalDateTime ***
//import java.util.*;
//
//@Controller
//public class PaymentCallbackController {
//
//    @Autowired private OrderRepository orderRepository;
//    @Autowired private CartService cartService; // Cần để xóa giỏ hàng
//    @Autowired private UserService userService; // Cần để lấy User
//
//    @Value("${vnpay.hashSecret}")
//    private String hashSecret;
//
//    // `/vnpay-return` giữ nguyên vì chỉ redirect dựa trên response code
//    @GetMapping("/vnpay-return")
//    public String vnpayReturn(HttpServletRequest request) {
//        String responseCode = request.getParameter("vnp_ResponseCode");
//        // Có thể thêm logic kiểm tra chữ ký ở đây để an toàn hơn chút
//        if ("00".equals(responseCode)) {
//            return "redirect:/order-success";
//        } else {
//            // Có thể lấy message lỗi từ VNPay để hiển thị
//            // String message = request.getParameter("vnp_Message");
//            return "redirect:/order-failed"; // ?reason=" + message;
//        }
//    }
//
//    // `/vnpay-ipn` (ĐÃ SỬA)
//    @GetMapping("/vnpay-ipn")
//    @Transactional // *** THÊM Transactional: Quan trọng để đảm bảo cập nhật DB nhất quán ***
//    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) throws UnsupportedEncodingException {
//
//        Map<String, String> params = new HashMap<>();
//        // Đọc parameters (giữ nguyên)
//        Enumeration<String> paramNames = request.getParameterNames();
//        while (paramNames.hasMoreElements()) {
//            String fieldName = paramNames.nextElement();
//            // *** SỬA: Dùng UTF-8 để decode parameter value cho đúng tiếng Việt ***
//            String fieldValue = URLDecoder.decode(request.getParameter(fieldName), StandardCharsets.UTF_8);
//            if ((fieldValue != null) && (fieldValue.length() > 0)) {
//                 params.put(fieldName, fieldValue);
//            }
//        }
//
//        String vnp_SecureHash = params.remove("vnp_SecureHash"); // Lấy hash gốc và xóa khỏi map
//        if (vnp_SecureHash == null) {
//            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum (Missing)\"}");
//        }
//
//
//        // --- XÁC THỰC CHỮ KÝ (Build lại hashData từ params đã decode) ---
//        List<String> fieldNames = new ArrayList<>(params.keySet());
//        Collections.sort(fieldNames);
//        StringBuilder hashData = new StringBuilder();
//        Iterator<String> itr = fieldNames.iterator();
//        while (itr.hasNext()) {
//            String fieldName = itr.next();
//            String fieldValue = params.get(fieldName);
//            if ((fieldValue != null) && (fieldValue.length() > 0)) {
//                // *** SỬA: Encode lại giá trị fieldValue bằng UTF-8 trước khi hash ***
//                hashData.append(fieldName);
//                hashData.append('=');
//                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8)); // Dùng UTF-8
//                if (itr.hasNext()) { // Chỉ thêm '&' nếu chưa phải cuối
//                    hashData.append('&');
//                }
//            }
//        }
//
//        String calculatedHash = VNPayUtil.hmacSHA512(hashSecret, hashData.toString());
//
//        if (!calculatedHash.equals(vnp_SecureHash)) {
//            System.err.println("VNPay IPN Invalid Checksum. Received: " + vnp_SecureHash + ", Calculated: " + calculatedHash + ", Data: " + hashData.toString());
//            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}");
//        }
//        System.out.println("VNPay IPN Checksum Validated.");
//
//        // --- KIỂM TRA LOGIC NGHIỆP VỤ ---
//        try {
//            Integer orderId = Integer.parseInt(params.get("vnp_TxnRef")); // Order ID là Integer
//            String responseCode = params.get("vnp_ResponseCode");
//            long vnpAmount = Long.parseLong(params.get("vnp_Amount")) / 100; // Số tiền (đơn vị VND)
//
//            System.out.println("Processing IPN for Order ID: " + orderId + ", Response Code: " + responseCode + ", Amount: " + vnpAmount);
//
//            Optional<Order> orderOpt = orderRepository.findById(orderId);
//
//            if (orderOpt.isEmpty()) {
//                System.err.println("VNPay IPN Error: Order not found for ID " + orderId);
//                return ResponseEntity.ok("{\"RspCode\":\"01\",\"Message\":\"Order not found\"}");
//            }
//
//            Order order = orderOpt.get();
//
//            // So sánh số tiền (dùng compareTo của BigDecimal)
//            if (order.getGrandTotal().compareTo(BigDecimal.valueOf(vnpAmount)) != 0) {
//                 System.err.println("VNPay IPN Error: Invalid Amount for Order ID " + orderId + ". Expected: " + order.getGrandTotal() + ", Received: " + vnpAmount);
//                return ResponseEntity.ok("{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}");
//            }
//
//            // Kiểm tra trạng thái thanh toán hiện tại
//            if (!"Unpaid".equalsIgnoreCase(order.getPaymentStatus())) {
//                 // Nếu đã Paid hoặc Refunded -> Báo thành công cho VNPay để tránh gọi lại, nhưng không làm gì thêm
//                 System.out.println("VNPay IPN Info: Order " + orderId + " already processed (Status: " + order.getPaymentStatus() + "). Responding Success to VNPay.");
//                 return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Order already confirmed\"}"); // VNPay yêu cầu trả 00 nếu đã xử lý
//            }
//
//            // --- CẬP NHẬT CSDL ---
//            if ("00".equals(responseCode)) {
//                System.out.println("VNPay IPN Success for Order ID: " + orderId + ". Updating status...");
//                // Thanh toán thành công
//                order.setPaymentStatus("Paid");
//                order.setOrderStatus("Confirmed"); // Chuyển sang Đã xác nhận (hoặc Processing tùy quy trình)
//                order.setPaidAt(LocalDateTime.now()); // *** SỬA: Dùng LocalDateTime ***
//                order.setTransactionID(params.get("vnp_TransactionNo")); // Lưu mã giao dịch VNPay
//
//                // Lấy User từ Order để xóa giỏ hàng
//                User user = order.getUser();
//                if (user != null && cartService != null) { // Kiểm tra cartService không null
//                    cartService.clearCart(user); // *** SỬA: Gọi clearCart với User ***
//                    System.out.println("Cart cleared for User ID: " + user.getId());
//                } else {
//                     System.err.println("VNPay IPN Warning: Could not clear cart. User or CartService is null for Order ID " + orderId);
//                }
//
//            } else {
//                // Thanh toán thất bại
//                 System.out.println("VNPay IPN Failed for Order ID: " + orderId + ". Status remains Unpaid.");
//                // Giữ 'Unpaid' hoặc chuyển 'Failed' tùy logic
//                 order.setPaymentStatus("Unpaid"); // Hoặc "Failed"
//                 // Có thể set OrderStatus thành "Cancelled" nếu thất bại?
//                 // order.setOrderStatus("Cancelled");
//            }
//
//            orderRepository.save(order); // Lưu thay đổi
//
//            System.out.println("VNPay IPN Processed Successfully for Order ID: " + orderId + ". Responding Success.");
//            // RspCode 00: Thành công (cho VNPay biết đã xử lý)
//            return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
//
//        } catch (Exception e) {
//            System.err.println("VNPay IPN Error: Unknown error during processing. " + e.getMessage());
//            e.printStackTrace();
//            // RspCode 99: Lỗi không xác định
//            return ResponseEntity.ok("{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}");
//        }
//    }
//}