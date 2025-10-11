package com.alotra.entity.promotion;

import com.alotra.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PromotionProducts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Thêm một ID tự tăng cho bảng nối

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PromotionID")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Product product;

    @Column(name = "DiscountPercentage", nullable = false)
    private Integer discountPercentage;
}