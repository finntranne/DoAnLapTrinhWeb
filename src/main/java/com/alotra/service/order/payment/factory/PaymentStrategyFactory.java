package com.alotra.service.order.payment.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alotra.config.VNPayConfig;
import com.alotra.enums.PaymentMethod;
import com.alotra.service.order.payment.sdk.VNPaySDK;
import com.alotra.service.order.payment.sdk.VietQRSDK;
import com.alotra.service.order.payment.strategy.*;

/**
 * PaymentStrategyFactory - Factory Pattern
 * Chịu trách nhiệm tạo và quản lý các instances của PaymentStrategy
 * dựa trên PaymentMethod được chỉ định
 * 
 * Được cấu hình như Spring Component để support dependency injection
 * Tích hợp với VnPayConfig để quản lý cấu hình VNPay
 */
@Component
public class PaymentStrategyFactory {
    
    @Autowired
    private VNPayConfig vnPayConfig;
    
    // Static instance cho non-spring usage khi cần singleton
    private static PaymentStrategyFactory instance;
    
    /**
     * Lấy singleton instance cho non-spring usage
     * Dùng khi gọi từ non-spring context (e.g. Entity transient methods)
     * Lưu ý: Singleton này sẽ sử dụng default VnPayConfig (chưa inject từ Spring)
     */
    public static PaymentStrategyFactory getInstance() {
        if (instance == null) {
            instance = new PaymentStrategyFactory();
            // VnPayConfig sẽ được autowired nếu có, nếu không sẽ null
            // Trong trường hợp này, VNPayAdapter sẽ dùng test credentials
        }
        return instance;
    }
    
    /**
     * Tạo PaymentStrategy tương ứng với PaymentMethod
     * Sử dụng VnPayConfig để quản lý cấu hình VNPay
     * @param paymentMethod Phương thức thanh toán
     * @return PaymentStrategy instance
     * @throws IllegalArgumentException nếu paymentMethod không được hỗ trợ
     */
    public PaymentStrategy createStrategy(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("PaymentMethod không được null");
        }
        
        return switch (paymentMethod) {
            case COD -> new CODStrategy();
            case VNPAY -> {
                // Sử dụng VnPayConfig được inject từ Spring
                if (vnPayConfig == null) {
                    throw new IllegalStateException("VnPayConfig chưa được inject. Kiểm tra application.properties");
                }
                VNPaySDK vnPaySDK = new VNPaySDK(vnPayConfig);
                yield new VNPayAdapter(vnPaySDK);
            }
            case BANK_TRANSFER -> new VietQRAdapter();
            default -> throw new IllegalArgumentException("Phương thức thanh toán không được hỗ trợ: " + paymentMethod);
        };
    }
    
    /**
     * Kiểm tra xem phương thức thanh toán có được hỗ trợ không
     * @param paymentMethod Phương thức thanh toán
     * @return true nếu được hỗ trợ
     */
    public static boolean isSupported(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return false;
        }
        
        return switch (paymentMethod) {
            case COD, VNPAY, BANK_TRANSFER -> true;
            default -> false;
        };
    }
}
