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

	@NotNull(message = "Vui lòng nhập số lượng")
	@Min(value = 0, message = "Số lượng phải lớn hơn hoặc bằng 0")
	private Integer stock;

	@Size(max = 100, message = "Mã SKU không được vượt quá 100 ký tự")
	private String sku;
}