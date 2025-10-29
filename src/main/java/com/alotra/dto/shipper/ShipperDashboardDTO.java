package com.alotra.dto.shipper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipperDashboardDTO {
    // Số đơn được gán (Confirmed - chưa bắt đầu giao)
    private Long assignedCount;
    
    // Số đơn đang giao (Delivering)
    private Long deliveringCount;
    
    // Số đơn hoàn thành hôm nay
    private Long completedTodayCount;
    
    // Tổng đơn trong tuần
    private Long weeklyCount;
    
    // Tổng đơn đã hoàn thành (tất cả thời gian)
    private Long totalCompletedCount;
    
    // Tỷ lệ giao thành công (%)
    private Double successRate;
    
    // Số ngày làm việc
    private Long workingDays;
}