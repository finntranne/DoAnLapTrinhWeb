package com.alotra.dto.shop;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alotra.enums.OrderStatus;
import com.alotra.enums.PaymentMethod;
import com.alotra.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopOrderDTO {
    private Integer orderId;
    private LocalDateTime orderDate;
    private String orderStatus;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal grandTotal;
    private String customerName;
    private String customerPhone;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String shipperName;
    private Integer totalItems;
}
