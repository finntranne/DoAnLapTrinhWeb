//package com.alotra.entity.product;
//
//<<<<<<< HEAD
//import com.alotra.entity.promotion.PromotionProduct;
//import com.alotra.entity.shop.Shop; // Import Shop entity
//import com.alotra.entity.user.Review;
//
//import jakarta.persistence.*;
//import lombok.*;
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.hibernate.annotations.CreationTimestamp;
//
//@Entity
//@Table(name = "Products")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class Product {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "ProductID")
//    private Integer productId;
////
////    // --- CÁC TRƯỜNG MỚI VÀ CẬP NHẬT ---
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(name = "ShopID", nullable = false)
////    @EqualsAndHashCode.Exclude
////    @ToString.Exclude
////    private Shop shop;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "CategoryID", nullable = false)
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Category category;
//
//    @Column(name = "ProductName", nullable = false, columnDefinition = "NVARCHAR(255)")
//    private String productName;
//
//    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
//    private String description;
//
//    @Column(name = "Status")
//    private Byte status; // TINYINT ánh xạ tốt hơn với Byte
//
//    @Column(name = "AverageRating")
//    private BigDecimal averageRating;
//
//    @Column(name = "TotalReviews")
//    private Integer totalReviews;
//
//    @Column(name = "ViewCount")
//    private Integer viewCount;
//
//    @Column(name = "SoldCount")
//    private Integer soldCount;
//    
//    @Column(name = "TotalLikes")
//    private Integer totalLikes;
//    
//    @Column(name = "BasePrice", columnDefinition = "DECIMAL(18, 2)") // <-- THÊM DÒNG NÀY
//    private BigDecimal basePrice; // Giá thấp nhất để sắp xếp
//    
//    @CreationTimestamp
//    @Column(name = "CreatedAt", nullable = false, updatable = false)
//    private Instant createdAt;
//
//    // --- CÁC MỐI QUAN HỆ GIỮ NGUYÊN ---
//    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Set<ProductVariant> productVariants;
//
//    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Set<ProductImage> productImages;
//    
//    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Set<PromotionProduct> promotionProducts;
//    
//    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
//    private List<Review> reviews;
//    
//    @ManyToMany(fetch = FetchType.LAZY)
//    @JoinTable(
//        name = "ProductAvailableToppings", // Name of the join table
//        joinColumns = @JoinColumn(name = "ProductID"),
//        inverseJoinColumns = @JoinColumn(name = "ToppingID")
//    )
//    @EqualsAndHashCode.Exclude // Important for ManyToMany
//    @ToString.Exclude        // Important for ManyToMany
//    private Set<Topping> availableToppings = new HashSet<>();
//=======
//import com.alotra.entity.shop.Shop;
//import com.alotra.entity.product.Category;
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.ToString;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Entity
//@Table(name = "Products", indexes = {
//    @Index(name = "IX_Products_ShopID", columnList = "ShopID"),
//    @Index(name = "IX_Products_CategoryID", columnList = "CategoryID"),
//    @Index(name = "IX_Products_Status", columnList = "Status")
//})
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@ToString(exclude = {"shop", "category", "variants", "images"})
//public class Product {
//    
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "ProductID")
//    private Integer productID;
//    
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "ShopID", nullable = false)
//    private Shop shop;
//    
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "CategoryID", nullable = false)
//    private Category category;
//    
//    @Column(name = "ProductName", nullable = false, length = 255)
//    private String productName;
//    
//    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
//    private String description;
//    
//    @Column(name = "Status", nullable = false)
//    private Byte status = 1; // 0: Inactive, 1: Active
//    
//    @Column(name = "AverageRating", precision = 3, scale = 2)
//    private BigDecimal averageRating = BigDecimal.ZERO;
//    
//    @Column(name = "TotalReviews")
//    private Integer totalReviews = 0;
//    
//    @Column(name = "ViewCount")
//    private Integer viewCount = 0;
//    
//    @Column(name = "SoldCount")
//    private Integer soldCount = 0;
//    
//    @Column(name = "CreatedAt", nullable = false)
//    private LocalDateTime createdAt = LocalDateTime.now();
//    
//    @Column(name = "UpdatedAt", nullable = false)
//    private LocalDateTime updatedAt = LocalDateTime.now();
//    
//    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<ProductVariant> variants = new ArrayList<>();
//    
//    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<ProductImage> images = new ArrayList<>();
//>>>>>>> lam
//}

package com.alotra.entity.product;

import com.alotra.entity.shop.Shop;
import com.alotra.entity.promotion.PromotionProduct;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Products", indexes = {
    @Index(name = "IX_Products_ShopID", columnList = "ShopID"),
    @Index(name = "IX_Products_CategoryID", columnList = "CategoryID"),
    @Index(name = "IX_Products_Status", columnList = "Status"),
    @Index(name = "IX_Products_SoldCount", columnList = "SoldCount DESC"),
    @Index(name = "IX_Products_AverageRating", columnList = "AverageRating DESC"),
    @Index(name = "IX_Products_TotalLikes", columnList = "TotalLikes DESC"),
    @Index(name = "IX_Products_BasePrice", columnList = "BasePrice")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"shop", "category", "variants", "images", "promotionProducts", "reviews", "availableToppings"})
@EqualsAndHashCode(exclude = {"shop", "category", "variants", "images", "promotionProducts", "reviews", "availableToppings"})
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
    private Byte status;

    @Column(name = "AverageRating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "TotalReviews")
    private Integer totalReviews = 0;

    @Column(name = "ViewCount")
    private Integer viewCount = 0;

    @Column(name = "SoldCount")
    private Integer soldCount = 0;

    // ✅ THÊM: Tổng số lượt thích
    @Column(name = "TotalLikes")
    private Integer totalLikes = 0;

    // ✅ THÊM: Giá thấp nhất để sắp xếp
    @Column(name = "BasePrice", precision = 18, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    // Quan hệ với ProductVariant
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductVariant> variants = new ArrayList<>();

    // Quan hệ với ProductImage
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();

    // ✅ THÊM: Quan hệ với PromotionProduct
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<PromotionProduct> promotionProducts = new HashSet<>();

    // ✅ THÊM: Quan hệ với Review
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    // ✅ THÊM: Quan hệ Many-to-Many với Topping
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "ProductAvailableToppings",
        joinColumns = @JoinColumn(name = "ProductID"),
        inverseJoinColumns = @JoinColumn(name = "ToppingID")
    )
    private Set<Topping> availableToppings = new HashSet<>();

    // Tự động quản lý timestamps
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = 1; // Mặc định Active
        }
        // Khởi tạo giá trị mặc định
        if (averageRating == null) averageRating = BigDecimal.ZERO;
        if (totalReviews == null) totalReviews = 0;
        if (viewCount == null) viewCount = 0;
        if (soldCount == null) soldCount = 0;
        if (totalLikes == null) totalLikes = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}