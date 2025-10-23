package com.alotra.entity.order;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.user.Customer;
import com.alotra.entity.user.Employee;

@Entity
@Table(name = "Orders")
@Getter // <-- Thay thế
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "orderId")
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
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<OrderItem> items = new HashSet<>();

    // === Thêm hàm trợ giúp (không bắt buộc nhưng nên có) ===
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
