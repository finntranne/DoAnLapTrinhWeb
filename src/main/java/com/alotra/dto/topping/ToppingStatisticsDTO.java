package com.alotra.dto.topping;

import com.alotra.entity.product.Topping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToppingStatisticsDTO {
    private Topping topping;
    private String approvalStatus; // "Đang chờ: Cập nhật", "Bị từ chối: Tạo mới", null
    private String activityStatus; // "Đang hoạt động", "Không hoạt động"
}