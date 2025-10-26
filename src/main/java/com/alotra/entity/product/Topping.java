//package com.alotra.entity.product;
//
//<<<<<<< HEAD
//import jakarta.persistence.*;
//=======
//import java.math.BigDecimal;
//
//import com.alotra.entity.product.Topping;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//>>>>>>> lam
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//<<<<<<< HEAD
//import java.math.BigDecimal;
//=======
//>>>>>>> lam
//
//@Entity
//@Table(name = "Toppings")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class Topping {
//<<<<<<< HEAD
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "ToppingID")
//    private Integer toppingId;
//
//    @Column(name = "ToppingName", nullable = false, unique = true, columnDefinition = "NVARCHAR(255)")
//    private String toppingName;
//
//    @Column(name = "AdditionalPrice", nullable = false)
//    private BigDecimal additionalPrice;
//
//    // --- ADD IMAGE URL ---
//    @Column(name = "ImageUrl", columnDefinition = "NVARCHAR(2083)") // Allow null if some toppings don't have images
//    private String imageUrl;
//    // ---------------------
//
//    @Column(name = "Status", nullable = false)
//    private Byte status; // 0: Inactive, 1: Active
//}
//=======
//    
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "ToppingID")
//    private Integer toppingID;
//    
//    @Column(name = "ToppingName", nullable = false, unique = true, length = 255)
//    private String toppingName;
//    
//    @Column(name = "AdditionalPrice", nullable = false, precision = 10, scale = 2)
//    private BigDecimal additionalPrice;
//    
//    @Column(name = "Status", nullable = false)
//    private Byte status = 1; // 0: Inactive, 1: Active
//    
//    @Column(name = "ImageURL", length = 500)
//    private String imageURL;
//}
//>>>>>>> lam



package com.alotra.entity.product; // Giữ package này

import java.math.BigDecimal;

import com.alotra.entity.shop.Shop;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Toppings",
    // *** THÊM UNIQUE CONSTRAINT NÀY ***
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_Topping_Shop_Name", columnNames = {"ShopID", "ToppingName"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"shop"}) // Thêm exclude
@EqualsAndHashCode(exclude = {"shop"}) // Thêm exclude
public class Topping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ToppingID")
    private Integer toppingID;
    
    // *** THÊM TRƯỜNG SHOPID NÀY ***
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopID") // Cho phép null nếu Admin tạo
    private Shop shop;

    @Column(name = "ToppingName", nullable = false, length = 255)
    private String toppingName;

    @Column(name = "AdditionalPrice", nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalPrice;

    @Column(name = "Status", nullable = false)
    private Byte status = 1;

    @Column(name = "ImageURL", length = 500)
    private String imageURL;
}