package com.alotra.entity.order;

import java.time.LocalDateTime;

import com.alotra.enums.PaymentMethod;
import com.alotra.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID")
    private Integer paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "transaction_code")
    private String transactionCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false, unique = true)
    private Order order;
    
    @Column(name = "payment_url", length = 2000)
    private String paymentUrl; // URL thanh toán online (VNPay, MOMO) - Tối đa 2000 ký tự
    
    @Column(name = "qr_code", length = 2000)
    private String qrCode; // QR code thanh toán (VietQR) - Tối đa 2000 ký tự
}
