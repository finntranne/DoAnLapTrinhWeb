package com.alotra.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "Toppings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Topping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ToppingID")
    private Integer toppingId;

    @Column(name = "ToppingName", nullable = false, unique = true)
    private String toppingName;

    @Column(name = "AdditionalPrice", nullable = false)
    private BigDecimal additionalPrice;

    // --- ADD IMAGE URL ---
    @Column(name = "ImageUrl") // Allow null if some toppings don't have images
    private String imageUrl;
    // ---------------------

    @Column(name = "Status", nullable = false)
    private Byte status; // 0: Inactive, 1: Active
}