package com.alotra.dto.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO để lưu trữ kết quả thanh toán từ các payment gateway
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {
    
    private boolean success;
    private String transactionCode;
    private String message;
    private String paymentUrl; // Dùng cho VNPay, Zalopay
    private String qrCode; // Dùng cho VietQR
    
    // Constructor cho các trường hợp đơn giản
    public PaymentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public PaymentResult(boolean success, String transactionCode, String message) {
        this.success = success;
        this.transactionCode = transactionCode;
        this.message = message;
    }
}
