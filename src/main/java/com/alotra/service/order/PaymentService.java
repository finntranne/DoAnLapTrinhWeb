package com.alotra.service.order;

import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.order.PaymentResult;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.Payment;
import com.alotra.enums.PaymentMethod;
import com.alotra.enums.PaymentStatus;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.PaymentRepository;
import com.alotra.service.order.payment.context.PaymentContext;
import com.alotra.service.order.payment.factory.PaymentStrategyFactory;
import com.alotra.service.order.payment.strategy.PaymentStrategy;

/**
 * PaymentService - Service xử lý các giao dịch thanh toán
 * Áp dụng các design pattern:
 * - Strategy Pattern: Sử dụng PaymentStrategy cho các phương thức thanh toán khác nhau
 * - Adapter Pattern: Adapter cho các SDK thanh toán (VNPay, VietQR, MOMO)
 * - Factory Pattern: PaymentStrategyFactory tạo strategy instances
 * - Context Pattern: PaymentContext quản lý strategy và thực hiện thanh toán
 */
@Service
@Transactional
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentStrategyFactory paymentStrategyFactory;
    
    /**
     * Tạo bản ghi Payment cho đơn hàng
     * @param order Đơn hàng cần thanh toán
     * @param paymentMethod Phương thức thanh toán
     * @return Payment entity đã lưu
     */
    public Payment createPayment(Order order, PaymentMethod paymentMethod) {
        // Kiểm tra phương thức thanh toán có được hỗ trợ không
        if (!PaymentStrategyFactory.isSupported(paymentMethod)) {
            throw new IllegalArgumentException("Phương thức thanh toán không được hỗ trợ: " + paymentMethod);
        }
        
        // Kiểm tra xem đơn hàng đã có payment chưa
        Optional<Payment> existingPayment = paymentRepository.findByOrder_OrderID(order.getOrderID());
        if (existingPayment.isPresent()) {
            throw new IllegalStateException("Đơn hàng đã có bản ghi thanh toán");
        }
        
        // Tạo Payment entity mới
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(paymentMethod);
        payment.setStatus(PaymentStatus.UNPAID);
        
        return paymentRepository.save(payment);
    }
    
    /**
     * Xử lý thanh toán cho đơn hàng
     * Sử dụng Strategy Pattern thông qua PaymentContext
     * @param orderId ID của đơn hàng
     * @return PaymentResult - Kết quả thanh toán
     */
    public PaymentResult processPayment(Integer orderId) {
        // Lấy bản ghi thanh toán
        Optional<Payment> paymentOpt = paymentRepository.findByOrder_OrderID(orderId);
        
        if (paymentOpt.isEmpty()) {
            return new PaymentResult(false, "Không tìm thấy bản ghi thanh toán cho đơn hàng");
        }
        
        Payment payment = paymentOpt.get();
        
        // Kiểm tra trạng thái thanh toán
        if (payment.getStatus() == PaymentStatus.PAID || 
            payment.getStatus() == PaymentStatus.REFUNDED) {
            return new PaymentResult(false, "Đơn hàng này đã được thanh toán rồi");
        }
        
        // Tạo PaymentContext với factory và thực hiện thanh toán
        PaymentContext context = new PaymentContext(payment.getMethod(), paymentStrategyFactory);
        PaymentResult result = context.executePayment(payment.getOrder());
        
        if (result.isSuccess()) {
            // Cập nhật Payment entity
            payment.setTransactionCode(result.getTransactionCode());
            payment.setPaymentUrl(result.getPaymentUrl());
            payment.setQrCode(result.getQrCode());
            payment.setStatus(PaymentStatus.PAID);
            
            paymentRepository.save(payment);
        }
        
        return result;
    }
    
    /**
     * Lấy thông tin thanh toán của đơn hàng
     * @param orderId ID của đơn hàng
     * @return Payment entity
     */
    public Optional<Payment> getPaymentByOrder(Integer orderId) {
        return paymentRepository.findByOrder_OrderID(orderId);
    }
    
    /**
     * Hoàn lại tiền cho đơn hàng
     * @param orderId ID của đơn hàng
     * @return true nếu hoàn tiền thành công
     */
    public boolean refundPayment(Integer orderId) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrder_OrderID(orderId);
        
        if (paymentOpt.isEmpty()) {
            return false;
        }
        
        Payment payment = paymentOpt.get();
        
        // Kiểm tra xem thanh toán đã được thực hiện chưa
        if (payment.getStatus() != PaymentStatus.PAID) {
            return false;
        }
        
        // Tạo Context với factory và thực hiện hoàn tiền
        PaymentContext context = new PaymentContext(payment.getMethod(), paymentStrategyFactory);
        boolean refundSuccess = context.executeRefund(payment.getTransactionCode());
        
        if (refundSuccess) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }
        
        return refundSuccess;
    }
    
    /**
     * Thay đổi phương thức thanh toán
     * @param orderId ID của đơn hàng
     * @param newPaymentMethod Phương thức thanh toán mới
     * @return Payment entity đã cập nhật
     */
    public Payment changePaymentMethod(Integer orderId, PaymentMethod newPaymentMethod) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrder_OrderID(orderId);
        
        if (paymentOpt.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy bản ghi thanh toán");
        }
        
        Payment payment = paymentOpt.get();
        
        // Chỉ có thể thay đổi nếu chưa thanh toán
        if (payment.getStatus() != PaymentStatus.UNPAID) {
            throw new IllegalStateException("Không thể thay đổi phương thức thanh toán cho đơn hàng đã thanh toán");
        }
        
        // Kiểm tra phương thức thanh toán mới
        if (!PaymentStrategyFactory.isSupported(newPaymentMethod)) {
            throw new IllegalArgumentException("Phương thức thanh toán không được hỗ trợ: " + newPaymentMethod);
        }
        
        payment.setMethod(newPaymentMethod);
        return paymentRepository.save(payment);
    }
    
    /**
     * Kiểm tra xem phương thức thanh toán có được hỗ trợ không
     * @param paymentMethod Phương thức thanh toán
     * @return true nếu được hỗ trợ
     */
    public boolean isPaymentMethodSupported(PaymentMethod paymentMethod) {
        return PaymentStrategyFactory.isSupported(paymentMethod);
    }
    
    /**
     * Lấy danh sách các phương thức thanh toán được hỗ trợ
     * @return PaymentMethod[]
     */
    public PaymentMethod[] getSupportedPaymentMethods() {
        return new PaymentMethod[]{
            PaymentMethod.COD,
            PaymentMethod.VNPAY,
            PaymentMethod.BANK_TRANSFER
        };
    }
    
    /**
     * Xác nhận thanh toán thành công từ VNPay callback
     * @param orderId ID đơn hàng
     * @param transactionNo VNPay transaction number
     * @param payDate Ngày giờ thanh toán từ VNPay
     */
    public void confirmPayment(Integer orderId, String transactionNo, String payDate) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrder_OrderID(orderId);
        
        if (paymentOpt.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy bản ghi thanh toán");
        }
        
        Payment payment = paymentOpt.get();
        payment.setTransactionCode(transactionNo);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        
        paymentRepository.save(payment);
    }
    
    /**
     * Đánh dấu thanh toán thất bại
     * @param orderId ID đơn hàng
     * @param errorMessage Lý do thất bại
     */
    public void failPayment(Integer orderId, String errorMessage) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrder_OrderID(orderId);
        
        if (paymentOpt.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy bản ghi thanh toán");
        }
        
        Payment payment = paymentOpt.get();
        payment.setStatus(PaymentStatus.UNPAID);  // Đặt về UNPAID để cho phép thử lại
        // Có thể thêm note hoặc error message nếu Payment entity hỗ trợ
        
        paymentRepository.save(payment);
    }
}
