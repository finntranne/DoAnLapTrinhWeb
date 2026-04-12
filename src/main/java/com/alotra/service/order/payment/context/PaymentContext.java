package com.alotra.service.order.payment.context;

import com.alotra.dto.order.PaymentResult;
import com.alotra.entity.order.Order;
import com.alotra.enums.PaymentMethod;
import com.alotra.service.order.payment.factory.PaymentStrategyFactory;
import com.alotra.service.order.payment.strategy.PaymentStrategy;

/**
 * PaymentContext - Context class trong Strategy Pattern
 * Chịu trách nhiệm:
 * - Chứa PaymentStrategy
 * - Thực hiện các thao tác thanh toán thông qua strategy
 * - Cho phép thay đổi strategy dynamically
 */
public class PaymentContext {
    
    private PaymentStrategy strategy;
    private PaymentStrategyFactory factory;
    
    /**
     * Constructor khởi tạo với PaymentMethod (sử dụng factory singleton)
     * Sử dụng cho entity Payment's transient methods
     * @param paymentMethod Phương thức thanh toán
     */
    public PaymentContext(PaymentMethod paymentMethod) {
        // Lấy singleton instance của factory với test credentials
        this.factory = PaymentStrategyFactory.getInstance();
        this.strategy = factory.createStrategy(paymentMethod);
    }
    
    /**
     * Constructor khởi tạo với PaymentMethod và Factory
     * @param paymentMethod Phương thức thanh toán
     * @param factory Factory để tạo strategy
     */
    public PaymentContext(PaymentMethod paymentMethod, PaymentStrategyFactory factory) {
        this.factory = factory;
        this.strategy = factory.createStrategy(paymentMethod);
    }
    
    /**
     * Constructor khởi tạo với PaymentStrategy instance
     * @param strategy PaymentStrategy instance
     */
    public PaymentContext(PaymentStrategy strategy) {
        this.strategy = strategy;
    }
    
    /**
     * Set strategy dynamically (cho phép thay đổi strategy tại runtime)
     * @param strategy PaymentStrategy mới
     */
    public void setStrategy(PaymentStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("PaymentStrategy không được null");
        }
        this.strategy = strategy;
    }
    
    /**
     * Set strategy dựa vào PaymentMethod
     * @param paymentMethod Phương thức thanh toán
     */
    public void setStrategy(PaymentMethod paymentMethod) {
        if (factory == null) {
            factory = PaymentStrategyFactory.getInstance();
        }
        this.strategy = factory.createStrategy(paymentMethod);
    }
    
    /**
     * Thực hiện thanh toán cho đơn hàng
     * @param order Đơn hàng cần thanh toán
     * @return Kết quả thanh toán
     */
    public PaymentResult executePayment(Order order) {
        if (strategy == null) {
            return new PaymentResult(false, "Strategy thanh toán chưa được thiết lập");
        }
        
        if (order == null) {
            return new PaymentResult(false, "Đơn hàng không được null");
        }
        
        return strategy.pay(order);
    }
    
    /**
     * Hoàn lại tiền cho giao dịch
     * @param transactionCode Mã giao dịch
     * @return true nếu hoàn tiền thành công
     */
    public boolean executeRefund(String transactionCode) {
        if (strategy == null) {
            return false;
        }
        
        return strategy.refund(transactionCode);
    }
    
    /**
     * Lấy phương thức thanh toán hiện tại
     * @return PaymentMethod
     */
    public PaymentMethod getCurrentPaymentMethod() {
        if (strategy == null) {
            return null;
        }
        return strategy.getMethod();
    }
    
    /**
     * Lấy strategy hiện tại
     * @return PaymentStrategy
     */
    public PaymentStrategy getStrategy() {
        return strategy;
    }
}
