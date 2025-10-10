package com.alotra.entity.order;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.user.Customer;
import com.alotra.entity.user.Employee;

@Entity
@Table(name = "Orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    @ManyToOne
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "EmployeeID")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "PromotionID")
    private Promotion promotion;

    @Column(name = "OrderDate", nullable = false)
    private Instant orderDate;

    @Column(name = "OrderStatus", nullable = false)
    private String orderStatus; // 'Pending', 'Processing', 'Delivering', 'Completed', 'Cancelled'

    @Column(name = "PaymentStatus", nullable = false)
    private String paymentStatus; // 'Unpaid', 'Paid'

    @Column(name = "PaymentMethod", nullable = false)
    private String paymentMethod; // 'Cash', 'Momo', 'VNPay', 'ZaloPay', 'BankTransfer'

    @Column(name = "PaidAt")
    private Instant paidAt;

    @Column(name = "ShippingAddress", nullable = false)
    private String shippingAddress;

    @Column(name = "RecipientName", nullable = false)
    private String recipientName;

    @Column(name = "RecipientPhone", nullable = false)
    private String recipientPhone;

    @Column(name = "Subtotal", nullable = false)
    private BigDecimal subtotal;

    @Column(name = "DiscountAmount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "ShippingFee", nullable = false)
    private BigDecimal shippingFee;

    @Column(name = "GrandTotal", nullable = false)
    private BigDecimal grandTotal;

    @Column(name = "Notes")
    private String notes;
}
