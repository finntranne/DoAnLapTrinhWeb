
package com.alotra.controller; // Giữ package này

import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
// Import entity đã merge
import com.alotra.entity.order.Order;
import com.alotra.entity.user.User; // Sử dụng User
import com.alotra.repository.cart.CartItemRepository;
import com.alotra.repository.cart.CartRepository;
// Import Repository và Service đã merge
import com.alotra.repository.order.OrderRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.user.UserService; // Sử dụng UserService
import com.alotra.util.VNPayUtil; // Giữ lại Util

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.transaction.annotation.Transactional; // *** THÊM Transactional ***

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime; // *** SỬA: Dùng LocalDateTime ***
import java.util.*;
import java.util.stream.Collectors;



@Controller
public class PaymentCallbackController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartService cartService; // Cần để xóa giỏ hàng
    @Autowired private UserService userService; // Cần để lấy User
    
    @Autowired private CartRepository cartRepository; // *** THÊM REPO ***
    @Autowired private CartItemRepository cartItemRepository; // *** THÊM REPO ***

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    // `/vnpay-return` giữ nguyên vì chỉ redirect dựa trên response code
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request) {
        String responseCode = request.getParameter("vnp_ResponseCode");
        // Có thể thêm logic kiểm tra chữ ký ở đây để an toàn hơn chút
        if ("00".equals(responseCode)) {
            return "redirect:/order-success";
        } else {
            // Có thể lấy message lỗi từ VNPay để hiển thị
            // String message = request.getParameter("vnp_Message");
            return "redirect:/order-failed"; // ?reason=" + message;
        }
    }

    // `/vnpay-ipn` (ĐÃ SỬA)
    @GetMapping("/vnpay-ipn")
    @Transactional // *** THÊM Transactional: Quan trọng để đảm bảo cập nhật DB nhất quán ***
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) throws UnsupportedEncodingException {

        Map<String, String> params = new HashMap<>();
        // Đọc parameters (giữ nguyên)
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String fieldName = paramNames.nextElement();
            // *** SỬA: Dùng UTF-8 để decode parameter value cho đúng tiếng Việt ***
            String fieldValue = URLDecoder.decode(request.getParameter(fieldName), StandardCharsets.UTF_8);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                 params.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = params.remove("vnp_SecureHash"); // Lấy hash gốc và xóa khỏi map
        if (vnp_SecureHash == null) {
            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum (Missing)\"}");
        }


        // --- XÁC THỰC CHỮ KÝ (Build lại hashData từ params đã decode) ---
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // *** SỬA: Encode lại giá trị fieldValue bằng UTF-8 trước khi hash ***
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8)); // Dùng UTF-8
                if (itr.hasNext()) { // Chỉ thêm '&' nếu chưa phải cuối
                    hashData.append('&');
                }
            }
        }

        String calculatedHash = VNPayUtil.hmacSHA512(hashSecret, hashData.toString());

        if (!calculatedHash.equals(vnp_SecureHash)) {
            System.err.println("VNPay IPN Invalid Checksum. Received: " + vnp_SecureHash + ", Calculated: " + calculatedHash + ", Data: " + hashData.toString());
            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}");
        }
        System.out.println("VNPay IPN Checksum Validated.");

        // --- KIỂM TRA LOGIC NGHIỆP VỤ ---
        try {
            Integer orderId = Integer.parseInt(params.get("vnp_TxnRef"));
            String responseCode = params.get("vnp_ResponseCode");
            long vnpAmount = Long.parseLong(params.get("vnp_Amount")) / 100;

            System.out.println("Processing IPN for Order ID: " + orderId + ", Response Code: " + responseCode + ", Amount: " + vnpAmount);

            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isEmpty()) {
                System.err.println("VNPay IPN Error: Order not found for ID " + orderId);
                return ResponseEntity.ok("{\"RspCode\":\"01\",\"Message\":\"Order not found\"}");
            }

            Order order = orderOpt.get();

            // So sánh số tiền (giữ nguyên)
            if (order.getGrandTotal().compareTo(BigDecimal.valueOf(vnpAmount)) != 0) {
                 System.err.println("VNPay IPN Error: Invalid Amount for Order ID " + orderId + ". Expected: " + order.getGrandTotal() + ", Received: " + vnpAmount);
                return ResponseEntity.ok("{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}");
            }

            // Kiểm tra trạng thái (giữ nguyên)
            if (!"Unpaid".equalsIgnoreCase(order.getPaymentStatus())) {
                 System.out.println("VNPay IPN Info: Order " + orderId + " already processed (Status: " + order.getPaymentStatus() + "). Responding Success to VNPay.");
                 return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Order already confirmed\"}");
            }

            // --- CẬP NHẬT CSDL ---
            if ("00".equals(responseCode)) {
                System.out.println("VNPay IPN Success for Order ID: " + orderId + ". Updating status...");
                // 1. Cập nhật Order (giữ nguyên)
                order.setPaymentStatus("Paid");
                order.setOrderStatus("Confirmed"); 
                order.setPaidAt(LocalDateTime.now()); 
                order.setTransactionID(params.get("vnp_TransactionNo")); 

                // 2. *** SỬA LOGIC XÓA GIỎ HÀNG ***
                // Thay vì xóa toàn bộ giỏ hàng, chỉ xóa các item đã thanh toán
                User user = order.getUser();
                if (user != null) {
                    // Lấy các variant ID từ đơn hàng vừa thanh toán
                    Set<Integer> variantIdsInOrder = order.getOrderDetails().stream()
                            .map(detail -> detail.getVariant().getVariantID())
                            .collect(Collectors.toSet());

                    if (!variantIdsInOrder.isEmpty()) {
                        // Lấy giỏ hàng của user
                        Optional<Cart> cartOpt = cartRepository.findByUser_Id(user.getId());
                        if (cartOpt.isPresent()) {
                            Cart cart = cartOpt.get();
                            // Tìm các CartItem trong giỏ hàng khớp với các variant đã mua
                            List<CartItem> itemsToDelete = cart.getItems().stream()
                                    .filter(ci -> variantIdsInOrder.contains(ci.getVariant().getVariantID()))
                                    .collect(Collectors.toList());
                            
                            if (!itemsToDelete.isEmpty()) {
                                System.out.println("VNPay IPN: Deleting " + itemsToDelete.size() + " paid items from cart...");
                                for (CartItem item : itemsToDelete) {
                                    item.getSelectedToppings().clear(); // Xóa quan hệ topping
                                    cart.removeItem(item);             // Xóa khỏi cart entity
                                    cartItemRepository.delete(item);   // Xóa khỏi DB
                                }
                                cartRepository.save(cart); // Lưu lại cart
                            }
                        }
                    }
                } else {
                     System.err.println("VNPay IPN Warning: Could not clear cart items. User is null for Order ID " + orderId);
                }
                
            } else {
                // Thanh toán thất bại (giữ nguyên)
                 System.out.println("VNPay IPN Failed for Order ID: " + orderId + ". Status remains Unpaid.");
                 order.setPaymentStatus("Unpaid"); 
            }

            orderRepository.save(order); // Lưu thay đổi (cho cả thành công và thất bại)

            System.out.println("VNPay IPN Processed Successfully for Order ID: " + orderId + ". Responding Success.");
            return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");

        } catch (Exception e) {
            System.err.println("VNPay IPN Error: Unknown error during processing. " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok("{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}");
        }
    }
}