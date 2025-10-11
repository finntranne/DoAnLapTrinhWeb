package com.alotra.entity.product;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

import com.alotra.entity.promotion.PromotionProduct;

@Entity
@Table(name = "Products") // Sửa lại tên bảng
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID") // Sửa lại tên cột
    private Integer productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryID", nullable = false) // Sửa lại tên cột join
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Category category;

    @Column(name = "ProductName", nullable = false) // Sửa lại tên cột
    private String productName;

    @Column(name = "Description")
    private String description;

    @Column(name = "Status", nullable = false)
    private Integer status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ProductVariant> productVariants;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ProductImage> productImages;
    
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<PromotionProduct> promotionProducts;
}