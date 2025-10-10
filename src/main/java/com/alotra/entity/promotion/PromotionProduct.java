package com.alotra.entity.promotion;


import com.alotra.entity.product.Product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PromotionProducts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionProduct {

    @EmbeddedId
    private PromotionProductId id;

    @ManyToOne
    @MapsId("promotionId")
    @JoinColumn(name = "PromotionID")
    private Promotion promotion;

    @ManyToOne
    @MapsId("productId")
    @JoinColumn(name = "ProductID")
    private Product product;

    @Column(name = "DiscountPercentage", nullable = false)
    private Integer discountPercentage;
}

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class PromotionProductId {
    private Integer promotionId;
    private Integer productId;
}
