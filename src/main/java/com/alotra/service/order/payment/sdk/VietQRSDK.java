package com.alotra.service.order.payment.sdk;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * SDK wrapper cho VietQR - QR Code Payment Solution
 * Tích hợp với VietQR API (img.vietqr.io) để tạo mã QR thanh toán
 */
public class VietQRSDK {
    
    // VietQR API endpoints
    private static final String QR_IMAGE_API_URL = "https://img.vietqr.io/image";
    
    // Cấu hình VietQR (có thể được cấu hình qua constructor)
    private String bankId;
    private String accountNo;
    private String accountName;
    private String template;
    
    /**
     * Constructor mặc định với thông tin tài khoản mặc định
     */
    public VietQRSDK() {
        this("970405", "4303205336551", "TRAN HUU THOAI", "compact");
    }
    
    /**
     * Constructor có tham số
     */
    public VietQRSDK(String bankId, String accountNo, String accountName, String template) {
        this.bankId = bankId;
        this.accountNo = accountNo;
        this.accountName = accountName;
        this.template = template;
    }
    
    /**
     * Tạo QR Code image URL cho thanh toán VietQR
     * @param orderId ID của đơn hàng
     * @param amount Số tiền thanh toán (VND)
     * @param description Mô tả thanh toán
     * @return VietQR QR Code image URL
     */
    public String requestPayment(String orderId, long amount, String description) {
        try {
            // Build VietQR image URL
            // Format: https://img.vietqr.io/image/{bankId}-{accountNo}-{template}.jpg?amount={amount}&addInfo={description}&accountName={NAME}
            String encodedDesc = URLEncoder.encode(description, "UTF-8");
            String encodedName = URLEncoder.encode(accountName, "UTF-8");
            
            return String.format("%s/%s-%s-%s.jpg?amount=%d&addInfo=%s&accountName=%s",
                    QR_IMAGE_API_URL,
                    bankId,
                    accountNo,
                    template,
                    amount,
                    encodedDesc,
                    encodedName);
                    
        } catch (UnsupportedEncodingException e) {
            // Fallback to non-encoded version
            return String.format("%s/%s-%s-%s.jpg?amount=%d&addInfo=%s&accountName=%s",
                    QR_IMAGE_API_URL,
                    bankId,
                    accountNo,
                    template,
                    amount,
                    description.replace(" ", "+"),
                    accountName.replace(" ", "+"));
        }
    }
    
    /**
     * Xác nhận thanh toán đã hoàn tất
     * @param transactionCode Mã giao dịch
     * @return true nếu xác nhận thành công
     */
    public boolean confirmPayment(String transactionCode) {
        // Simulate: confirm payment from VietQR
        if (transactionCode == null || transactionCode.isEmpty()) {
            return false;
        }
        // Simulate successful confirmation
        return true;
    }
    
    /**
     * Hủy yêu cầu thanh toán
     * @param transactionCode Mã giao dịch cần hủy
     * @return true nếu hủy thành công
     */
    public boolean cancelOrder(String transactionCode) {
        // Simulate: cancel payment request
        if (transactionCode == null || transactionCode.isEmpty()) {
            return false;
        }
        // Simulate successful cancellation
        return true;
    }
    
    // Getters and Setters
    public String getBankId() {
        return bankId;
    }
    
    public void setBankId(String bankId) {
        this.bankId = bankId;
    }
    
    public String getAccountNo() {
        return accountNo;
    }
    
    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getTemplate() {
        return template;
    }
    
    public void setTemplate(String template) {
        this.template = template;
    }
}
