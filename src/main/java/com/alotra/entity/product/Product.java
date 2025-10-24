package com.alotra.entity.product;

import com.alotra.entity.promotion.PromotionProduct;
import com.alotra.entity.shop.Shop; // Import Shop entity
import com.alotra.entity.user.Review;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "Products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    private Integer productId;
//
//    // --- CÁC TRƯỜNG MỚI VÀ CẬP NHẬT ---
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "ShopID", nullable = false)
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Category category;

    @Column(name = "ProductName", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String productName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "Status")
    private Byte status; // TINYINT ánh xạ tốt hơn với Byte

    @Column(name = "AverageRating")
    private BigDecimal averageRating;

    @Column(name = "TotalReviews")
    private Integer totalReviews;

    @Column(name = "ViewCount")
    private Integer viewCount;

    @Column(name = "SoldCount")
    private Integer soldCount;
    
    @Column(name = "TotalLikes")
    private Integer totalLikes;
    
    @Column(name = "BasePrice", columnDefinition = "DECIMAL(18, 2)") // <-- THÊM DÒNG NÀY
    private BigDecimal basePrice; // Giá thấp nhất để sắp xếp
    
    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private Instant createdAt;

    // --- CÁC MỐI QUAN HỆ GIỮ NGUYÊN ---
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ProductVariant> productVariants;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ProductImage> productImages;
    
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<PromotionProduct> promotionProducts;
    
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Review> reviews;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "ProductAvailableToppings", // Name of the join table
        joinColumns = @JoinColumn(name = "ProductID"),
        inverseJoinColumns = @JoinColumn(name = "ToppingID")
    )
    @EqualsAndHashCode.Exclude // Important for ManyToMany
    @ToString.Exclude        // Important for ManyToMany
    private Set<Topping> availableToppings = new HashSet<>();
}