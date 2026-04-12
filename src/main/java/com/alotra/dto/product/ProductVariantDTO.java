package com.alotra.dto.product;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDTO {

	private Integer variantId;

	@NotNull(message = "Vui lòng chọn kích cỡ")
	private Integer sizeId;

	@NotNull(message = "Vui lòng nhập giá sản phẩm")
	@DecimalMin(value = "0.0", message = "Giá phải lớn hơn hoặc bằng 0")
	private BigDecimal price;

}