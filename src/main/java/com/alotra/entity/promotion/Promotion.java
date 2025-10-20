package com.alotra.entity.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.enums.DiscountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "Promotions", indexes = {
    @Index(name = "IX_Promotions_PromoCode", columnList = "PromoCode"),
    @Index(name = "IX_Promotions_Status", columnList = "Status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"createdByUserID", "createdByShopID"})
public class Promotion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromotionID")
    private Integer promotionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByUserID", nullable = false)
    private User createdByUserID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByShopID")
    private Shop createdByShopID;
    
    @Column(name = "PromotionName", nullable = false, length = 255)
    private String promotionName;
    
    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;
    
    @Column(name = "PromoCode", unique = true, length = 50)
    private String promoCode;
    
    @Column(name = "DiscountType", nullable = false, length = 20)
    private String discountType; // Percentage, FixedAmount, FreeShip
    
    @Column(name = "DiscountValue", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;
    
    @Column(name = "MaxDiscountAmount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;
    
    @Column(name = "StartDate", nullable = false)
    private LocalDateTime startDate;
    
    @Column(name = "EndDate", nullable = false)
    private LocalDateTime endDate;
    
    @Column(name = "MinOrderValue", precision = 10, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;
    
    @Column(name = "UsageLimit")
    private Integer usageLimit;
    
    @Column(name = "UsedCount")
    private Integer usedCount = 0;
    
    @Column(name = "Status", nullable = false)
    private Byte status = 1; // 0: Inactive, 1: Active
    
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
