
package com.alotra.entity.product; // Giữ package này

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
@ToString(exclude = {"shop"}) 
@EqualsAndHashCode(exclude = {"shop"}) 
public class Topping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ToppingID")
    private Integer toppingID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopID") 
    private Shop shop;

    @Column(name = "ToppingName", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String toppingName;

    @Column(name = "Price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "Status", nullable = false)
    private Byte status = 1;

    @Column(name = "ImageURL", length = 500)
    private String imageURL;
    
   
}