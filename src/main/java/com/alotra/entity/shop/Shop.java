package com.alotra.entity.shop;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Shops")
@Data
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShopID")
    private Integer shopId;

    @Column(name = "ShopName", nullable = false)
    private String shopName;

    // Các trường khác của Shop...
}
