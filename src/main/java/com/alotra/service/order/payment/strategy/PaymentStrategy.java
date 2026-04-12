package com.alotra.service.order.payment.strategy;

import com.alotra.dto.order.PaymentResult;
import com.alotra.entity.order.Order;
import com.alotra.enums.PaymentMethod;

/**
 * Strategy interface định nghĩa cách thức thanh toán
 * Tuân theo Strategy Pattern
 */
public interface PaymentStrategy {
    
    /**
     * Thực hiện thanh toán cho đơn hàng
     * @param order Đơn hàng cần thanh toán
     * @return Kết quả thanh toán
     */
    PaymentResult pay(Order order);
    
    /**
     * Hoàn lại tiền (environment, nếu hỗ trợ)
     * @param transactionCode Mã giao dịch
     * @return Kết quả hoàn tiền
     */
    boolean refund(String transactionCode);
    
    /**
     * Lấy phương thức thanh toán
     * @return PaymentMethod
     */
    PaymentMethod getMethod();
}
