package com.alotra.entity.product;

import java.math.BigDecimal;
import java.util.List;

import com.alotra.entity.shop.Shop;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "Products")
public class Product {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
	private int productId;
	
	@ManyToOne
    @JoinColumn(name = "ShopID", nullable = false)
    private Shop shop;

    @ManyToOne
    @JoinColumn(name = "CategoryID", nullable = false)
    private Category category;

    @Column(name = "ProductName", columnDefinition = "NVARCHAR(MAX)")
    private String productName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "Status")
    private Byte status; 

    @Column(name = "AverageRating")
    private BigDecimal averageRating;

    @Column(name = "TotalReviews")
    private Integer totalReviews;

    @Column(name = "ViewCount")
    private Integer viewCount;

    @Column(name = "SoldCount")
    private Integer soldCount;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductImage> images;

    
}
