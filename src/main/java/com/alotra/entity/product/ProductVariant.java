package com.alotra.entity.product;

import com.alotra.entity.order.OrderDetail; // Thêm import này
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.Set; // Thêm import này

@Entity
@Table(name = "ProductVariants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VariantID")
    private Integer variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Product product;

    @ManyToOne
    @JoinColumn(name = "SizeID", nullable = false)
    private Size size;

    @Column(name = "Price", nullable = false)
    private BigDecimal price;

    @Column(name = "Stock", nullable = false)
    private int stock;

    // === THÊM MỐI QUAN HỆ NÀY VÀO ===
    @OneToMany(mappedBy = "variant")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<OrderDetail> orderDetails;
}