package com.alotra.dto.shop;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopDashboardDTO {
    private Integer shopId;
    private String shopName;
    private String logoUrl;
    private Integer totalProducts;
    private Integer activeProducts;
    private Integer pendingApprovals;
    private Long totalOrders;
    private Long pendingOrders;
    private Long deliveringOrders;
    private BigDecimal totalRevenue;
    private BigDecimal thisMonthRevenue;
}
