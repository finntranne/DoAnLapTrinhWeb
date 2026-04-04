package com.alotra.model;

import com.alotra.entity.product.Product;
import lombok.Data;

@Data
public class ProductSaleDTO {
    private Product product;
    private Long totalSold;
    private Integer discountPercentage;
    private Double avgRating;
    private Long reviewCount;
    private Long likeCount;

    public ProductSaleDTO(Product product, Long totalSold, Integer discountPercentage) {
        this(product, totalSold, discountPercentage, 0.0, 0L, 0L);
    }

    public ProductSaleDTO(Product product, Long totalSold, Integer discountPercentage, Double avgRating, Long reviewCount,
            Long likeCount) {
        this.product = product;
        this.totalSold = totalSold != null ? totalSold : 0L;
        this.discountPercentage = discountPercentage;
        this.avgRating = avgRating != null ? avgRating : 0.0;
        this.reviewCount = reviewCount != null ? reviewCount : 0L;
        this.likeCount = likeCount != null ? likeCount : 0L;
    }
}
