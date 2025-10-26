//package com.alotra.entity.shop;
//
//<<<<<<< HEAD
//import jakarta.persistence.*;
//import lombok.Data;
//
//@Entity
//@Table(name = "Shops")
//@Data
//public class Shop {
//
//=======
//import com.alotra.entity.product.Product;
//import com.alotra.entity.promotion.Promotion;
//import com.alotra.entity.user.User;
//import com.alotra.enums.ShopStatus;
//
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
//@Table(name = "Shops", indexes = {
//    @Index(name = "IX_Shops_UserID", columnList = "UserID"),
//    @Index(name = "IX_Shops_Status", columnList = "Status")
//})
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@ToString(exclude = {"user", "products", "promotions"})
//public class Shop {
//    
//>>>>>>> lam
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "ShopID")
//    private Integer shopId;
//<<<<<<< HEAD
//
//    @Column(name = "ShopName", nullable = false)
//    private String shopName;
//
//    // Các trường khác của Shop...
//}
//=======
//    
//    @OneToOne
//    @JoinColumn(name = "UserID", nullable = false, unique = true)
//    private User user;
//    
//    @Column(name = "ShopName", nullable = false, unique = true, length = 255)
//    private String shopName;
//    
//    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
//    private String description;
//    
//    @Column(name = "LogoURL", length = 500)
//    private String logoURL;
//    
//    @Column(name = "CoverImageURL", length = 500)
//    private String coverImageURL;
//    
//    @Column(name = "Address", nullable = false, length = 500)
//    private String address;
//    
//    @Column(name = "PhoneNumber", nullable = false, length = 20)
//    private String phoneNumber;
//    
//    @Column(name = "Status", nullable = false)
//    private Byte status = 0; // 0: Pending, 1: Active, 2: Suspended
//    
//    @Column(name = "CommissionRate", precision = 5, scale = 2)
//    private BigDecimal commissionRate = new BigDecimal("5.00");
//    
//    @Column(name = "CreatedAt", nullable = false)
//    private LocalDateTime createdAt = LocalDateTime.now();
//    
//    @Column(name = "UpdatedAt", nullable = false)
//    private LocalDateTime updatedAt = LocalDateTime.now();
//    
//    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
//    private List<Product> products = new ArrayList<>();
//    
//    @OneToMany(mappedBy = "createdByShopID", cascade = CascadeType.ALL)
//    private List<Promotion> promotions = new ArrayList<>();
//}
//>>>>>>> lam


package com.alotra.entity.shop; // Giữ package này

import com.alotra.entity.product.Product;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.user.User;
// Bỏ import com.alotra.enums.ShopStatus; nếu dùng Byte

import jakarta.persistence.*; // Import đầy đủ JPA annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Shops", indexes = { // Giữ lại indexes từ nhánh lam
    @Index(name = "IX_Shops_UserID", columnList = "UserID"),
    @Index(name = "IX_Shops_Status", columnList = "Status"),
    @Index(name = "IX_Shops_ShopName", columnList = "ShopName") // Thêm index cho tên Shop
})
@Data
@NoArgsConstructor
@AllArgsConstructor
// Thêm Excludes cho tất cả quan hệ
@ToString(exclude = {"user", "products", "promotions"})
@EqualsAndHashCode(exclude = {"user", "products", "promotions"})
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShopID") // Khớp DB
    private Integer shopId; // Giữ tên nhất quán

    @OneToOne(fetch = FetchType.LAZY) // Thêm LAZY
    @JoinColumn(name = "UserID", nullable = false, unique = true) // Khớp DB
    private User user; // Chủ shop

    @Column(name = "ShopName", nullable = false, unique = true, length = 255) // Khớp DB
    private String shopName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)") // Khớp DB
    private String description;

    @Column(name = "LogoURL", length = 500) // Khớp DB
    private String logoURL;

    @Column(name = "CoverImageURL", length = 500) // Khớp DB
    private String coverImageURL;

    @Column(name = "Address", nullable = false, length = 500) // Khớp DB
    private String address;

    @Column(name = "PhoneNumber", nullable = false, length = 20) // Khớp DB
    private String phoneNumber;

    @Column(name = "Status", nullable = false) // Khớp DB
    private Byte status = 0; // 0: Pending, 1: Active, 2: Suspended - Giữ mặc định

    @Column(name = "CommissionRate", precision = 5, scale = 2) // Khớp DB
    private BigDecimal commissionRate = new BigDecimal("5.00"); // Giữ mặc định

    @Column(name = "CreatedAt", nullable = false, updatable = false) // Khớp DB, thêm updatable=false
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false) // Khớp DB
    private LocalDateTime updatedAt;

    // Quan hệ này có thể không cần thiết ở đây nếu không dùng tới, nhưng giữ lại từ nhánh lam
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY) // Thêm LAZY
    private List<Product> products = new ArrayList<>();

    // Quan hệ này có thể không cần thiết ở đây nếu không dùng tới, nhưng giữ lại từ nhánh lam
    @OneToMany(mappedBy = "createdByShopID", cascade = CascadeType.ALL, fetch = FetchType.LAZY) // Thêm LAZY
    private List<Promotion> promotions = new ArrayList<>();

    // Tự động quản lý timestamps
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (commissionRate == null) {
            commissionRate = new BigDecimal("5.00"); // Đảm bảo giá trị mặc định
        }
        if (status == null) {
             status = 0; // Mặc định là Pending
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}