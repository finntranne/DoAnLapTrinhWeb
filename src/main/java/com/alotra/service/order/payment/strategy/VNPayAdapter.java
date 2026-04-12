package com.alotra.service.order.payment.strategy;

import com.alotra.dto.order.PaymentResult;
import com.alotra.entity.order.Order;
import com.alotra.enums.PaymentMethod;
import com.alotra.service.order.payment.sdk.VNPaySDK;

/**
 * VNPayAdapter - Adapter cho VNPay SDK
 * Tuân theo Adapter Pattern - chuyển đổi interface của VNPaySDK
 * thành interface PaymentStrategy
 * 
 * VNPaySDK được inject từ PaymentStrategyFactory
 */
public class VNPayAdapter implements PaymentStrategy {
    
    private final VNPaySDK vnPaySDK;
    
    /**
     * Constructor nhận VNPaySDK đã được cấu hình
     */
    public VNPayAdapter(VNPaySDK vnPaySDK) {
        this.vnPaySDK = vnPaySDK;
    }
    
    @Override
    public PaymentResult pay(Order order) {
        // Adapt VNPaySDK method vào PaymentStrategy interface
        
        try {
            // Tính tổng số tiền cần thanh toán (VND)
            long totalAmount = order.getItems().stream()
                    .mapToLong(item -> item.getVariant().getPrice()
                            .multiply(new java.math.BigDecimal(item.getQuantity()))
                            .longValue())
                    .sum();
            
            // Cộng phí vận chuyển
            totalAmount += order.getShippingFee().longValue();
            
            // Mô tả đơn hàng
            String orderInfo = "Thanh toán đơn hàng #" + order.getOrderID();
            
            // Gọi VNPaySDK để tạo URL thanh toán với default IP (127.0.0.1)
            // IP sẽ được cập nhật từ HttpServletRequest ở PaymentController khi có request
            String paymentUrl = vnPaySDK.createPaymentUrl(
                    order.getOrderID().toString(),
                    totalAmount,
                    orderInfo,
                    "127.0.0.1"  // Default IP cho adapter
            );
            
            PaymentResult result = new PaymentResult();
            result.setSuccess(true);
            result.setTransactionCode("VNPAY-" + order.getOrderID() + "-" + System.currentTimeMillis());
            result.setPaymentUrl(paymentUrl);
            result.setMessage("Vui lòng truy cập URL để thanh toán VNPay");
            
            return result;
            
        } catch (Exception e) {
            return new PaymentResult(false, "Lỗi khi tạo URL thanh toán VNPay: " + e.getMessage());
        }
    }
    
    @Override
    public boolean refund(String transactionCode) {
        // Refund được xử lý qua API khác, không hỗ trợ ở adapter
        return false;
    }
    
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.VNPAY;
    }
}
