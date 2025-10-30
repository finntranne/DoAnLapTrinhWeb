package com.alotra.dto.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.alotra.enums.DiscountType;
import com.alotra.enums.ActionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequestDTO {

	private Integer promotionId;

	@NotBlank(message = "Promotion name is required")
	@Size(max = 255, message = "Promotion name must not exceed 255 characters")
	private String promotionName;

	@Size(max = 5000, message = "Description must not exceed 5000 characters")
	private String description;

	@Size(max = 50, message = "Promo code must not exceed 50 characters")
	private String promoCode;

	@NotNull(message = "Discount type is required")
	private String discountType;

	@NotNull(message = "Discount value is required")
	@DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
	private BigDecimal discountValue;

	private BigDecimal maxDiscountAmount;

	@NotNull(message = "Start date is required")
	private LocalDateTime startDate;

	@NotNull(message = "End date is required")
	private LocalDateTime endDate;

	@DecimalMin(value = "0.0", message = "Min order value cannot be negative")
	private BigDecimal minOrderValue = BigDecimal.ZERO;

	private Integer usageLimit;

	private List<Integer> productIds;

	private ActionType actionType;

	private Byte status = 1; // 0: Inactive, 1: Active

	@NotBlank
	private String promotionType = "ORDER"; // 'ORDER' hoặc 'PRODUCT'

	@Valid // Đảm bảo các item trong list cũng được validate
	private List<ProductDiscountDTO> productDiscounts;

	@AssertTrue(message = "End date must be after start date")
	public boolean isEndDateAfterStartDate() {
		if (startDate == null || endDate == null)
			return true;
		return endDate.isAfter(startDate);
	}

	// *** FIX: CHO PHÉP 0 (SẼ ĐƯỢC NORMALIZE THÀNH NULL) ***
	@AssertTrue(message = "Số lượt sử dụng phải là số dương hoặc để trống (không giới hạn)")
	public boolean isUsageLimitValid() {
		// Cho phép null (không giới hạn) HOẶC >= 0 (0 sẽ được normalize thành null)
		return usageLimit == null || usageLimit >= 0;
	}
}
