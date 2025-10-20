package com.alotra.entity.product;

import com.alotra.entity.shop.Shop;
import com.alotra.entity.product.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Products", indexes = {
    @Index(name = "IX_Products_ShopID", columnList = "ShopID"),
    @Index(name = "IX_Products_CategoryID", columnList = "CategoryID"),
    @Index(name = "IX_Products_Status", columnList = "Status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"shop", "category", "variants", "images"})
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    private Integer productID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopID", nullable = false)
    private Shop shop;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryID", nullable = false)
    private Category category;
    
    @Column(name = "ProductName", nullable = false, length = 255)
    private String productName;
    
    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;
    
    @Column(name = "Status", nullable = false)
    private Byte status = 1; // 0: Inactive, 1: Active
    
    @Column(name = "AverageRating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;
    
    @Column(name = "TotalReviews")
    private Integer totalReviews = 0;
    
    @Column(name = "ViewCount")
    private Integer viewCount = 0;
    
    @Column(name = "SoldCount")
    private Integer soldCount = 0;
    
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();
}