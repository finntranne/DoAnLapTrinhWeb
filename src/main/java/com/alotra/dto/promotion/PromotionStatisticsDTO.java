package com.alotra.dto.promotion;

import com.alotra.entity.promotion.Promotion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionStatisticsDTO {
	private Promotion promotion;
	private String approvalStatus; // "Đang chờ: Cập nhật", null
	private String activityStatus; // "Đang hoạt động", "Đã kết thúc", "Không hoạt động"
}