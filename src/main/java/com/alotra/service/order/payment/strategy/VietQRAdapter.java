package com.alotra.service.order.payment.strategy;

import com.alotra.dto.order.PaymentResult;
import com.alotra.entity.order.Order;
import com.alotra.enums.PaymentMethod;
import com.alotra.service.order.payment.sdk.VietQRSDK;

/**
 * VietQRAdapter - Adapter cho VietQR SDK
 * Tuân theo Adapter Pattern - chuyển đổi interface của VietQRSDK
 * thành interface PaymentStrategy
 */
public class VietQRAdapter implements PaymentStrategy {
    
    private final VietQRSDK vietQRSDK;
    
    public VietQRAdapter() {
        this.vietQRSDK = new VietQRSDK();
    }
    
    public VietQRAdapter(VietQRSDK vietQRSDK) {
        this.vietQRSDK = vietQRSDK;
    }
    
    @Override
    public PaymentResult pay(Order order) {
        // Adapt VietQRSDK method vào PaymentStrategy interface
        
        try {
            // Tính tổng số tiền cần thanh toán (VND)
            long totalAmount = order.getItems().stream()
                    .mapToLong(item -> item.getVariant().getPrice()
                            .multiply(new java.math.BigDecimal(item.getQuantity()))
                            .longValue())
                    .sum();
            
            // Cộng phí vận chuyển
            totalAmount += order.getShippingFee().longValue();
            
            // Mô tả thanh toán
            String description = "Thanh toán đơn hàng #" + order.getOrderID();
            
            // Gọi VietQRSDK để tạo QR code
            String qrData = vietQRSDK.requestPayment(
                    order.getOrderID().toString(),
                    totalAmount,
                    description
            );
            
            PaymentResult result = new PaymentResult();
            result.setSuccess(true);
            result.setTransactionCode("VIETQR-" + order.getOrderID() + "-" + System.currentTimeMillis());
            result.setQrCode(qrData);
            result.setMessage("Quét mã QR để thanh toán VietQR");
            
            return result;
            
        } catch (Exception e) {
            return new PaymentResult(false, "Lỗi khi tạo QR code VietQR: " + e.getMessage());
        }
    }
    
    @Override
    public boolean refund(String transactionCode) {
        // Adapt VietQRSDK cancel method
        try {
            return vietQRSDK.cancelOrder(transactionCode);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.BANK_TRANSFER; // VietQR dùng cho chuyển khoản
    }
}
