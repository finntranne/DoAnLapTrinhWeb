package com.alotra.model;

import com.alotra.entity.product.Product;
import lombok.Data;

@Data
public class ProductSaleDTO {
    private Product product;
    private Long totalSold;
    private Integer discountPercentage; // Có thể null nếu không giảm giá
    private Double avgRating;  // Thêm
    private Long reviewCount;

    public ProductSaleDTO(Product product, Long totalSold, Integer discountPercentage) {
        this.product = product;
        this.totalSold = (totalSold != null) ? totalSold : 0L; // <-- Xử lý NULL ở đây
        this.discountPercentage = discountPercentage;
    }
}