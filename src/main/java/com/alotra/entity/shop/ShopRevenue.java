package com.alotra.entity.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alotra.entity.order.Order;

@Entity
@Table(name = "ShopRevenue", indexes = {
    @Index(name = "IX_ShopRevenue_ShopID", columnList = "ShopID"),
    @Index(name = "IX_ShopRevenue_RecordedAt", columnList = "RecordedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"shop", "order"})
public class ShopRevenue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RevenueID")
    private Integer revenueID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopID", nullable = false)
    private Shop shop;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;
    
    @Column(name = "OrderAmount", nullable = false, precision = 12, scale = 2)
    private BigDecimal orderAmount;
    
    @Column(name = "CommissionAmount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;
    
    @Column(name = "NetRevenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal netRevenue;
    
    @Column(name = "RecordedAt", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();
}
