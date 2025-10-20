package com.alotra.dto.product;

import java.math.BigDecimal;

import com.alotra.enums.ApprovalStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatisticsDTO {
    private Integer productId;
    private String productName;
    private String primaryImageUrl;
    private Integer soldCount;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer viewCount;
    private BigDecimal minPrice;
    private String status;
    private String approvalStatus;
}
