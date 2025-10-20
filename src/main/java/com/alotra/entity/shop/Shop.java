package com.alotra.entity.shop;

import com.alotra.entity.product.Product;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.user.User;
import com.alotra.enums.ShopStatus;

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
@Table(name = "Shops", indexes = {
    @Index(name = "IX_Shops_UserID", columnList = "UserID"),
    @Index(name = "IX_Shops_Status", columnList = "Status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "products", "promotions"})
public class Shop {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShopID")
    private Integer shopId;
    
    @OneToOne
    @JoinColumn(name = "UserID", nullable = false, unique = true)
    private User user;
    
    @Column(name = "ShopName", nullable = false, unique = true, length = 255)
    private String shopName;
    
    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;
    
    @Column(name = "LogoURL", length = 500)
    private String logoURL;
    
    @Column(name = "CoverImageURL", length = 500)
    private String coverImageURL;
    
    @Column(name = "Address", nullable = false, length = 500)
    private String address;
    
    @Column(name = "PhoneNumber", nullable = false, length = 20)
    private String phoneNumber;
    
    @Column(name = "Status", nullable = false)
    private Byte status = 0; // 0: Pending, 1: Active, 2: Suspended
    
    @Column(name = "CommissionRate", precision = 5, scale = 2)
    private BigDecimal commissionRate = new BigDecimal("5.00");
    
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
    private List<Product> products = new ArrayList<>();
    
    @OneToMany(mappedBy = "createdByShopID", cascade = CascadeType.ALL)
    private List<Promotion> promotions = new ArrayList<>();
}