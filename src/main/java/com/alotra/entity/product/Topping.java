package com.alotra.entity.product;

import java.math.BigDecimal;

import com.alotra.entity.product.Topping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "Toppings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Topping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ToppingID")
    private Integer toppingID;
    
    @Column(name = "ToppingName", nullable = false, unique = true, length = 255)
    private String toppingName;
    
    @Column(name = "AdditionalPrice", nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalPrice;
    
    @Column(name = "Status", nullable = false)
    private Byte status = 1; // 0: Inactive, 1: Active
    
    @Column(name = "ImageURL", length = 500)
    private String imageURL;
}
