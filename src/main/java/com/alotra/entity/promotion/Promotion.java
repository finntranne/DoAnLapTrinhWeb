//package com.alotra.entity.promotion;
//
//<<<<<<< HEAD
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//import lombok.ToString;
//
//import java.time.LocalDate;
//import java.util.Set;
//
//@Entity
//@Table(name = "Promotions")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class Promotion {
//
//=======
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//import com.alotra.entity.shop.Shop;
//import com.alotra.entity.user.User;
//import com.alotra.enums.DiscountType;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.Table;
//import jakarta.persistence.Index;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.ToString;
//
//@Entity
//@Table(name = "Promotions", indexes = {
//    @Index(name = "IX_Promotions_PromoCode", columnList = "PromoCode"),
//    @Index(name = "IX_Promotions_Status", columnList = "Status")
//})
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@ToString(exclude = {"createdByUserID", "createdByShopID"})
//public class Promotion {
//    
//>>>>>>> lam
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "PromotionID")
//    private Integer promotionId;
//<<<<<<< HEAD
//
//    @Column(name = "PromotionName", nullable = false, columnDefinition = "NVARCHAR(255)")
//    private String promotionName;
//
//    @Column(name = "Description", columnDefinition = "NVARCHAR(1000)")
//    private String description;
//
//    @Column(name = "StartDate", nullable = false)
//    private LocalDate startDate;
//
//    @Column(name = "EndDate", nullable = false)
//    private LocalDate endDate;
//
//    @Column(name = "Status", nullable = false)
//    private Byte status; // 0: Inactive, 1: Active
//    
//    @OneToMany(mappedBy = "promotion", fetch = FetchType.LAZY)
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Set<PromotionProduct> promotionProducts;
//=======
//    
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "CreatedByUserID", nullable = false)
//    private User createdByUserID;
//    
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "CreatedByShopID")
//    private Shop createdByShopID;
//    
//    @Column(name = "PromotionName", nullable = false, length = 255)
//    private String promotionName;
//    
//    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
//    private String description;
//    
//    @Column(name = "PromoCode", unique = true, length = 50)
//    private String promoCode;
//    
//    @Column(name = "DiscountType", nullable = false, length = 20)
//    private String discountType; // Percentage, FixedAmount, FreeShip
//    
//    @Column(name = "DiscountValue", nullable = false, precision = 10, scale = 2)
//    private BigDecimal discountValue;
//    
//    @Column(name = "MaxDiscountAmount", precision = 10, scale = 2)
//    private BigDecimal maxDiscountAmount;
//    
//    @Column(name = "StartDate", nullable = false)
//    private LocalDateTime startDate;
//    
//    @Column(name = "EndDate", nullable = false)
//    private LocalDateTime endDate;
//    
//    @Column(name = "MinOrderValue", precision = 10, scale = 2)
//    private BigDecimal minOrderValue = BigDecimal.ZERO;
//    
//    @Column(name = "UsageLimit")
//    private Integer usageLimit;
//    
//    @Column(name = "UsedCount")
//    private Integer usedCount = 0;
//    
//    @Column(name = "Status", nullable = false)
//    private Byte status = 1; // 0: Inactive, 1: Active
//    
//    @Column(name = "CreatedAt", nullable = false)
//    private LocalDateTime createdAt = LocalDateTime.now();
//>>>>>>> lam
//}

package com.alotra.entity.promotion; // Giữ package này

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
// Bỏ import com.alotra.enums.DiscountType; nếu dùng String

import jakarta.persistence.*; // Import đầy đủ JPA annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

@Entity
@Table(name = "Promotions", indexes = { // Giữ lại indexes từ nhánh lam
    @Index(name = "IX_Promotions_PromoCode", columnList = "PromoCode"),
    @Index(name = "IX_Promotions_Status", columnList = "Status"),
    @Index(name = "IX_Promotions_EndDate", columnList = "EndDate") // Thêm index cho EndDate nếu thường lọc/sắp xếp
})
@Data
@NoArgsConstructor
@AllArgsConstructor
// Thêm Excludes cho quan hệ LAZY
@ToString(exclude = {"createdByUserID", "createdByShopID"})
@EqualsAndHashCode(exclude = {"createdByUserID", "createdByShopID"}) // Thêm Exclude
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromotionID") // Khớp DB
    private Integer promotionId; // Giữ tên nhất quán (chữ thường d)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByUserID", nullable = false) // Khớp DB
    private User createdByUserID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByShopID") // Khớp DB
    private Shop createdByShopID; // Có thể null nếu Admin tạo

    @Column(name = "PromotionName", nullable = false, length = 255) // Khớp DB
    private String promotionName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)") // Khớp DB
    private String description;

    @Column(name = "PromoCode", unique = true, length = 50) // Khớp DB
    private String promoCode;
    
    @Column(name = "PromotionType", nullable = false, length = 20)
    private String promotionType = "ORDER"; // 'ORDER' hoặc 'PRODUCT'

    // Sử dụng String cho DiscountType như trong nhánh lam và DB
    @Column(name = "DiscountType", nullable = true, length = 20)
    private String discountType; // 'Percentage', 'FixedAmount', 'FreeShip'

    @Column(name = "DiscountValue", nullable = true, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "MaxDiscountAmount", precision = 10, scale = 2) // Khớp DB
    private BigDecimal maxDiscountAmount; // Dùng cho Percentage

    @Column(name = "StartDate", nullable = false) // Khớp DB
    private LocalDateTime startDate;

    @Column(name = "EndDate", nullable = false) // Khớp DB
    private LocalDateTime endDate;

    @Column(name = "MinOrderValue", precision = 10, scale = 2) // Khớp DB
    private BigDecimal minOrderValue = BigDecimal.ZERO; // Giữ mặc định

    @Column(name = "UsageLimit")
    private Integer usageLimit;

    @Column(name = "UsedCount") // Khớp DB
    private Integer usedCount = 0; // Giữ mặc định

    @Column(name = "Status", nullable = false) // Khớp DB
    private Byte status; // Mặc định được set ở @PrePersist

    @Column(name = "CreatedAt", nullable = false, updatable = false) // Khớp DB, thêm updatable=false
    private LocalDateTime createdAt;

    // Bỏ @OneToMany Set<PromotionProduct> từ HEAD vì không cần thiết
    // Mối quan hệ này được quản lý bởi PromotionProduct entity

    // Tự động gán giá trị mặc định khi tạo mới
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = 1; // Mặc định là Active khi tạo (trừ khi Service set thành 0 cho approval)
        }
        if (usedCount == null) {
            usedCount = 0;
        }
         if (minOrderValue == null) {
            minOrderValue = BigDecimal.ZERO;
        }
    }
}