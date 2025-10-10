package com.alotra.entity.product;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ProductVariants") // Sửa lại tên bảng
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VariantID") // Sửa lại tên cột
    private Integer variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false) // Sửa lại tên cột join
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Product product;

    @ManyToOne
    @JoinColumn(name = "SizeID", nullable = false) // Sửa lại tên cột join
    private Size size;

    @Column(name = "Price", nullable = false) // Sửa lại tên cột
    private BigDecimal price;

    @Column(name = "Stock", nullable = false) // Sửa lại tên cột
    private int stock;
}