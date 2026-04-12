package com.alotra.dto.topping;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;


@Data
public class ToppingRequestDTO {
    
    private Integer toppingId;
    
    private Integer shopId;
    
    @NotBlank(message = "Tên topping không được để trống")
    @Size(max = 255)
    private String toppingName;

    @NotNull(message = "Giá thêm không được để trống")
    @DecimalMin(value = "0.0", message = "Giá phải lớn hơn hoặc bằng 0")
    private BigDecimal additionalPrice;
    
    @Size(max = 500, message = "URL hình ảnh quá dài")
    private String imageURL;
    
    private String existingImageUrl;

    private MultipartFile imageFile;
}