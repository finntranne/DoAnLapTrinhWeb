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

    private Integer variantId; // null nếu CREATE, có giá trị nếu UPDATE

    @NotNull(message = "Size is required")
    private Integer sizeId;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;
}
