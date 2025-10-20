package com.alotra.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopRevenueDTO {
    private LocalDateTime date;
    private Long totalOrders;
    private BigDecimal orderAmount;
    private BigDecimal commissionAmount;
    private BigDecimal netRevenue;
}