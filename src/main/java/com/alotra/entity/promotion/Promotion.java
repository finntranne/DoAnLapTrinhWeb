package com.alotra.entity.promotion;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.alotra.entity.product.Product;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.Role;

import jakarta.persistence.CascadeType;
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
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "Promotions")
public class Promotion {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromotionID")
	private int promotiontId;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByShopID", nullable = true) 
    private Shop shop;
	
	@Column(name = "PromoCode")
	private String promoCode;
	
	@Column(name = "DiscountType", columnDefinition = "NVARCHAR(100)")
	private String discountType;
	
	@Column(name = "DiscountValue")
	private Double discountValue;
	
	@Column(name = "StartDate")
	private LocalDate startDate;
	
	@Column(name = "EndDate")
	private LocalDate endDate;
	
	@Column(name = "MinOrderValue")
	private Double minOrderValue;
	
	@Column(name = "Status")
	private int status;
	
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
        name = "Promotion_Shop_Applicability",
        joinColumns = @JoinColumn(name = "PromotionID"),
        inverseJoinColumns = @JoinColumn(name = "ShopID")
    )
    private Set<Shop> shops = new HashSet<>();
	
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
        name = "Promotion_Product_Applicability",
        joinColumns = @JoinColumn(name = "PromotionID"),
        inverseJoinColumns = @JoinColumn(name = "ProductID")
    )
    private Set<Product> products = new HashSet<>();
	
}
