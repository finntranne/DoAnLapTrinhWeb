package com.alotra.dto.promotion;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProductDiscountDTO {
    
    @NotNull(message = "ID sản phẩm không được rỗng")
    private Integer productId;
    
    @NotNull(message = "Phần trăm giảm giá không được rỗng")
    @Min(value = 1, message = "Giảm giá phải ít nhất 1%")
    @Max(value = 100, message = "Giảm giá không được quá 100%")
    private Integer discountPercentage;
}