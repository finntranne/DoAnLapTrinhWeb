package com.alotra.dto.topping;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class ToppingRequestDTO {
    
    private Integer toppingId;
    
    @NotBlank(message = "Tên topping không được để trống")
    @Size(max = 255)
    private String toppingName;

    @NotNull(message = "Giá thêm không được để trống")
    @DecimalMin(value = "0.0", message = "Giá phải lớn hơn hoặc bằng 0")
    private BigDecimal additionalPrice;
    
    @Size(max = 500, message = "URL hình ảnh quá dài")
    private String imageURL;

 // *** THÊM TRƯỜNG NÀY ĐỂ NHẬN FILE TỪ FORM ***
    @JsonIgnore // Bỏ qua trường này khi serialize sang JSON
    private MultipartFile imageFile;
}