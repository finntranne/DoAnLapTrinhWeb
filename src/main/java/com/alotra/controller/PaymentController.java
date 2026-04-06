package com.alotra.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alotra.config.VNPayConfig;
import com.alotra.dto.order.PaymentResult;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.Payment;
import com.alotra.enums.PaymentMethod;
import com.alotra.repository.order.OrderRepository;
import com.alotra.service.order.PaymentService;
import com.alotra.service.order.payment.sdk.VNPaySDK;
import jakarta.servlet.http.HttpServletRequest;

/**
 * PaymentController - REST API cho Payment System
 * Cung cấp các endpoints để:
 * - Tạo payment record
 * - Xử lý thanh toán
 * - Hoàn tiền
 * - Thay đổi phương thức thanh toán
 * - Lấy danh sách phương thức thanh toán được hỗ trợ
 * 
 * Áp dụng: Strategy Pattern, Adapter Pattern, Factory Pattern
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private VNPayConfig vnPayConfig;
    
    /**
     * VNPaySDK instance tạo từ VNPayConfig
     */
    private VNPaySDK getVNPaySDK() {
        return new VNPaySDK(vnPayConfig);
    }
    
    /**
     * Tạo payment record cho đơn hàng
     * POST /api/payments/create
     * 
     * Request body:
     * {
     *   "orderId": 1,
     *   "paymentMethod": "VNPAY"  // COD, VNPAY, MOMO, BANK_TRANSFER, ZALOPAY
     * }
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(
            @RequestBody Map<String, Object> requestBody,
            Authentication authentication) {
        try {
            Integer orderId = (Integer) requestBody.get("orderId");
            String paymentMethodStr = (String) requestBody.get("paymentMethod");
            
            if (orderId == null || paymentMethodStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "orderId và paymentMethod là bắt buộc"));
            }
            
            // Lấy đơn hàng
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy đơn hàng"));
            }
            
            Order order = orderOpt.get();
            PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentMethodStr);
            
            // Tạo payment
            Payment payment = paymentService.createPayment(order, paymentMethod);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo payment thành công",
                    "paymentId", payment.getPaymentId(),
                    "paymentMethod", payment.getMethod()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Xử lý thanh toán cho đơn hàng
     * POST /api/payments/process/{orderId}
     * 
     * Response:
     * {
     *   "success": true,
     *   "transactionCode": "VNPAY-1-1234567890",
     *   "paymentUrl": "https://sandbox.vnpayment.vn/pay?...", // Cho VNPay, MOMO
     *   "qrCode": "https://api.vietqr.io/qr?...",           // Cho VietQR
     *   "message": "Vui lòng truy cập URL để thanh toán"
     * }
     */
    @PostMapping("/process/{orderId}")
    public ResponseEntity<?> processPayment(@PathVariable Integer orderId) {
        try {
            PaymentResult result = paymentService.processPayment(orderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("transactionCode", result.getTransactionCode());
            response.put("message", result.getMessage());
            
            // Thêm payment URL nếu có (VNPay, MOMO)
            if (result.getPaymentUrl() != null) {
                response.put("paymentUrl", result.getPaymentUrl());
            }
            
            // Thêm QR code nếu có (VietQR)
            if (result.getQrCode() != null) {
                response.put("qrCode", result.getQrCode());
            }
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Lấy thông tin payment của đơn hàng
     * GET /api/payments/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getPayment(@PathVariable Integer orderId) {
        try {
            Optional<Payment> paymentOpt = paymentService.getPaymentByOrder(orderId);
            
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy payment"));
            }
            
            Payment payment = paymentOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", payment.getPaymentId());
            response.put("orderId", orderId);
            response.put("method", payment.getMethod());
            response.put("status", payment.getStatus());
            response.put("transactionCode", payment.getTransactionCode());
            response.put("paidAt", payment.getPaidAt());
            response.put("paymentUrl", payment.getPaymentUrl());
            response.put("qrCode", payment.getQrCode());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Hoàn lại tiền cho đơn hàng
     * POST /api/payments/refund/{orderId}
     */
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<?> refundPayment(@PathVariable Integer orderId) {
        try {
            boolean refundSuccess = paymentService.refundPayment(orderId);
            
            if (refundSuccess) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Hoàn tiền thành công"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "Hoàn tiền thất bại"
                        ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Thay đổi phương thức thanh toán
     * PATCH /api/payments/{orderId}/method
     * 
     * Request body:
     * {
     *   "paymentMethod": "MOMO"
     * }
     */
    @PatchMapping("/{orderId}/method")
    public ResponseEntity<?> changePaymentMethod(
            @PathVariable Integer orderId,
            @RequestBody Map<String, String> requestBody) {
        try {
            String paymentMethodStr = requestBody.get("paymentMethod");
            
            if (paymentMethodStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "paymentMethod là bắt buộc"));
            }
            
            PaymentMethod newMethod = PaymentMethod.valueOf(paymentMethodStr);
            
            if (!paymentService.isPaymentMethodSupported(newMethod)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Phương thức thanh toán không được hỗ trợ"));
            }
            
            Payment updated = paymentService.changePaymentMethod(orderId, newMethod);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thay đổi phương thức thanh toán thành công",
                    "newMethod", updated.getMethod()
            ));
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Lấy danh sách các phương thức thanh toán được hỗ trợ
     * GET /api/payments/supported-methods
     */
    @GetMapping("/supported-methods")
    public ResponseEntity<?> getSupportedPaymentMethods() {
        PaymentMethod[] methods = paymentService.getSupportedPaymentMethods();
        
        Map<String, Object> response = new HashMap<>();
        response.put("supportedMethods", methods);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Kiểm tra phương thức thanh toán có được hỗ trợ không
     * GET /api/payments/check-support/{paymentMethod}
     */
    @GetMapping("/check-support/{paymentMethod}")
    public ResponseEntity<?> checkPaymentMethodSupport(@PathVariable String paymentMethod) {
        try {
            PaymentMethod method = PaymentMethod.valueOf(paymentMethod);
            boolean supported = paymentService.isPaymentMethodSupported(method);
            
            return ResponseEntity.ok(Map.of(
                    "paymentMethod", method,
                    "supported", supported
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Phương thức thanh toán không hợp lệ: " + paymentMethod));
        }
    }
    
    /**
     * VNPay Callback Handler
     * GET /vnpay-return
     * 
     * VNPay sẽ redirect khách hàng đến endpoint này sau khi thanh toán
     * Endpoint này xác minh callback hash và cập nhật trạng thái payment
     * 
     * Các parameters từ VNPay:
     * - vnp_ResponseCode: 00 = success, other = failed
     * - vnp_TransactionNo: VNPay transaction ID
     * - vnp_Amount: Số tiền thanh toán (x100)
     * - vnp_PayDate: Ngày giờ thanh toán
     * - vnp_BankCode: Mã ngân hàng khách hàng sử dụng
     * - vnp_SecureHash: Chữ ký xác minh từ VNPay
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(HttpServletRequest request) {
        try {
            // Lấy tất cả parameters từ request
            Map<String, String> vnpParams = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values.length > 0) {
                    vnpParams.put(key, values[0]);
                }
            });
            
            // Lấy hash từ parameters
            String vnpSecureHash = vnpParams.get("vnp_SecureHash");
            if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing vnp_SecureHash", "success", false));
            }
            
            // Xóa hash khỏi params để validate
            vnpParams.remove("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHashType");
            
            // Xác minh hash
            if (!getVNPaySDK().validatePaymentHash(vnpParams, vnpSecureHash)) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Invalid checksum",
                                "success", false,
                                "message", "Chữ ký thanh toán không hợp lệ"
                        ));
            }
            
            // Lấy response code
            String vnpResponseCode = vnpParams.get("vnp_ResponseCode");
            String vnpOrderInfo = vnpParams.get("vnp_OrderInfo");
            String vnpTransactionNo = vnpParams.get("vnp_TransactionNo");
            String vnpPayDate = vnpParams.get("vnp_PayDate");
            String vnpAmount = vnpParams.get("vnp_Amount");
            
            // Parse order ID từ vnp_TxnRef
            String vnpTxnRef = vnpParams.get("vnp_TxnRef");
            Integer orderId;
            try {
                orderId = Integer.parseInt(vnpTxnRef);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid order ID", "success", false));
            }
            
            // Lấy payment record
            Optional<Payment> paymentOpt = paymentService.getPaymentByOrder(orderId);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Payment not found", "success", false));
            }
            
            Payment payment = paymentOpt.get();
            
            // Kiểm tra response code
            if (getVNPaySDK().isPaymentSuccess(vnpResponseCode)) {
                // Thanh toán thành công
                paymentService.confirmPayment(orderId, vnpTransactionNo, vnpPayDate);
                
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Thanh toán thành công",
                        "orderId", orderId,
                        "transactionNo", vnpTransactionNo,
                        "amount", Long.parseLong(vnpAmount) / 100 // Convert back to VND
                ));
            } else {
                // Thanh toán thất bại
                String errorMessage = getVNPayErrorMessage(vnpResponseCode);
                
                // Cập nhật trạng thái thanh toán thất bại
                paymentService.failPayment(orderId, errorMessage);
                
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Map.of(
                                "success", false,
                                "message", "Thanh toán thất bại: " + errorMessage,
                                "orderId", orderId,
                                "responseCode", vnpResponseCode
                        ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Error processing VNPay callback",
                            "message", e.getMessage(),
                            "success", false
                    ));
        }
    }
    
    /**
     * Ánh xạ VNPay response codes thành error messages tiếng Việt
     */
    private String getVNPayErrorMessage(String responseCode) {
        return switch (responseCode) {
            case "01" -> "Yêu cầu được gửi từ IP không được phép";
            case "02" -> "Merchant được lock";
            case "03" -> "URL không hợp lệ";
            case "04" -> "Số tiền không hợp lệ";
            case "05" -> "Không tìm thấy giao dịch";
            case "06" -> "Chữ ký không hợp lệ";
            case "07" -> "Không đủ tiền trong tài khoản";
            case "08" -> "Giao dịch bị từ chối";
            case "09" -> "Tài khoản không được phép thanh toán trực tuyến";
            case "10" -> "Thẻ hết hạn hoặc bị khóa";
            case "11" -> "Giao dịch bị hủy bởi người dùng";
            case "12" -> " Giao dịch đang chờ xử lý";
            case "13" -> "Lỗi máy chủ";
            default -> "Lỗi không xác định (" + responseCode + ")";
        };
    }
}
