package com.alotra.service.order.payment.strategy;

import com.alotra.dto.order.PaymentResult;
import com.alotra.entity.order.Order;
import com.alotra.enums.PaymentMethod;

/**
 * CODStrategy - Cash On Delivery
 * Phương thức thanh toán tiền mặt khi nhận hàng
 * Tuân theo Strategy Pattern
 */
public class CODStrategy implements PaymentStrategy {
    
    @Override
    public PaymentResult pay(Order order) {
        // COD không cần xử lý thanh toán online
        // Chỉ cần ghi nhận yêu cầu thanh toán COD
        
        String message = "Đơn hàng #" + order.getOrderID() 
                         + " đã được ghi nhận cho thanh toán COD";
        
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setTransactionCode("COD-" + order.getOrderID() + "-" + System.currentTimeMillis());
        result.setMessage(message);
        
        return result;
    }
    
    @Override
    public boolean refund(String transactionCode) {
        // COD không hỗ trợ refund online
        // Refund được xử lý thủ công khi trả hàng
        return false;
    }
    
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.COD;
    }
}
