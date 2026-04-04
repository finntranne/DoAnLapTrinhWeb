package com.alotra.entity.shop; // Giữ package này

import com.alotra.entity.common.MessageEntity;
import com.alotra.entity.location.Address;
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
@ToString(exclude = {"user", "products"})
@EqualsAndHashCode(exclude = {"user", "products"})
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShopID") // Khớp DB
    private Integer shopId; // Giữ tên nhất quán

    @OneToOne(fetch = FetchType.LAZY) // Thêm LAZY
    @JoinColumn(name = "UserID", nullable = false, unique = true) // Khớp DB
    private User user; // Chủ shop

    @Column(name = "ShopName", nullable = false, unique = true, length = 255, columnDefinition = "NVARCHAR(255)") // Khớp DB
    private String shopName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)") // Khớp DB
    private String description;

    @Column(name = "LogoURL", length = 500) // Khớp DB
    private String logoURL;

    @Column(name = "CoverImageURL", length = 500) // Khớp DB
    private String coverImageURL;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "AddressID", nullable = false)
    private Address address;

    @Column(name = "PhoneNumber", nullable = false, length = 20) // Khớp DB
    private String phoneNumber;

    @Column(name = "Status", nullable = false) // Khớp DB
    private Byte status = 0; // 0: Pending, 1: Active, 2: Suspended - Giữ mặc định

    @Column(name = "CreatedAt", nullable = false, updatable = false) // Khớp DB, thêm updatable=false
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false) 
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY) 
    private List<Product> products = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
