package com.alotra.dto.product;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.alotra.enums.ActionType;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDTO {

    private Integer productId;

    @NotNull(message = "Vui lòng chọn danh mục sản phẩm")
    private Integer categoryId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String productName;

    @Size(max = 5000, message = "Mô tả sản phẩm không được vượt quá 5000 ký tự")
    private String description;
    
    @JsonIgnore
    @Size(max = 10, message = "Chỉ được tải lên tối đa 10 hình ảnh")
    private List<MultipartFile> images;

    private List<String> existingImageUrls;
    
    private Integer primaryImageIndex;

    @NotNull(message = "Vui lòng thêm ít nhất một biến thể sản phẩm")
    @Size(min = 1, message = "Phải có ít nhất một biến thể sản phẩm")
    @Valid
    private List<ProductVariantDTO> variants;
    
    private List<String> newImageUrls;
}
