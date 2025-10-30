
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

    @Column(name = "ToppingName", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String toppingName;

    @Column(name = "AdditionalPrice", nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalPrice;

    @Column(name = "Status", nullable = false)
    private Byte status = 1;

    @Column(name = "ImageURL", length = 500)
    private String imageURL;
}