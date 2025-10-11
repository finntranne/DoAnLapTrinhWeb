package com.alotra.model;

import com.alotra.entity.product.Product;
import lombok.Data;

@Data
public class ProductSaleDTO {
    private Product product;
    private Long totalSold;
    private Integer discountPercentage; // Có thể null nếu không giảm giá

    public ProductSaleDTO(Product product, Long totalSold, Integer discountPercentage) {
        this.product = product;
        this.totalSold = totalSold;
        this.discountPercentage = discountPercentage;
    }
}