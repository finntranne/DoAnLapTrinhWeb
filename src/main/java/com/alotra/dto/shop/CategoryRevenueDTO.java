package com.alotra.dto.shop;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRevenueDTO {
    private String categoryName;
    private BigDecimal totalGrossRevenue = BigDecimal.ZERO; // Tổng doanh thu (Gross)
    private BigDecimal totalNetRevenue = BigDecimal.ZERO;   // Doanh thu thực nhận (Net)
    // Optional: Add totalOrders if needed
     private Long totalOrders = 0L;
}