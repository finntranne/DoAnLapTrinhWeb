package com.alotra.entity.draft;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.Topping;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.shop.Shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "PromotionDrafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDraft implements DraftEntity{

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromotionDraftID") // Khớp DB
    private Integer promotionDraftId; // Giữ tên nhất quán (chữ thường d)
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PromotionID", nullable = true)
	private Promotion promotion;

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

    
    @Column(name = "MaxDiscountAmount", precision = 10, scale = 2) // Khớp DB
    private BigDecimal maxDiscountAmount;

    @Column(name = "MinOrderValue", precision = 10, scale = 2) 
    private BigDecimal minOrderValue = BigDecimal.ZERO; 

    @Column(name = "UsageLimit")
    private Integer usageLimit = null;
    
    @ManyToMany
    @JoinTable(
        name = "PromotionDraftProduct",
        joinColumns = @JoinColumn(name = "PromotionDraftID"),
        inverseJoinColumns = @JoinColumn(name = "ProductID")
    )
    private List<Product> products = new ArrayList<>();
    
	@Override
	public Integer getId() {
		return promotionDraftId;
	}

}
