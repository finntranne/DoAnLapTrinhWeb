package com.alotra.entity.promotion; // Giữ package này

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.alotra.entity.product.Product;
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
@ToString(exclude = { "createdByShopID"})
@EqualsAndHashCode(exclude = {"createdByShopID"}) // Thêm Exclude
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromotionID") // Khớp DB
    private Integer promotionId; // Giữ tên nhất quán (chữ thường d)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByShopID") // Khớp DB
    private Shop createdByShopID; // Có thể null nếu Admin tạo

    @Column(name = "PromotionName", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)") // Khớp DB
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


    @Column(name = "StartDate", nullable = false) // Khớp DB
    private LocalDateTime startDate;

    @Column(name = "EndDate", nullable = false) // Khớp DB
    private LocalDateTime endDate;

    @Column(name = "Status", nullable = false) // Khớp DB
    private Byte status; // Mặc định được set ở @PrePersist

    @Column(name = "CreatedAt", nullable = false, updatable = false) // Khớp DB, thêm updatable=false
    private LocalDateTime createdAt;
    
    @Column(name = "MaxDiscountAmount", precision = 10, scale = 2) // Khớp DB
    private BigDecimal maxDiscountAmount;

    @Column(name = "MinOrderValue", precision = 10, scale = 2) 
    private BigDecimal minOrderValue = BigDecimal.ZERO; 

    @Column(name = "UsageLimit")
    private Integer usageLimit;

    @Column(name = "UsedCount") // Khớp DB
    private Integer usedCount = 0; // Giữ mặc định
    
    @ManyToMany
    @JoinTable(
        name = "PromotionProduct",
        joinColumns = @JoinColumn(name = "PromotionID"),
        inverseJoinColumns = @JoinColumn(name = "ProductID")
    )
    private List<Product> products = new ArrayList<>();

}