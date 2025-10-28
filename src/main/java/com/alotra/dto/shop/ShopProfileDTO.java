package com.alotra.dto.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopProfileDTO {
    
    private Integer shopId;
    
    @NotBlank(message = "Tên cửa hàng không được để trống")
    @Size(min = 3, max = 255, message = "Tên cửa hàng phải từ 3-255 ký tự")
    private String shopName;
    
    @Size(max = 5000, message = "Mô tả không được quá 5000 ký tự")
    private String description;
    
    // URLs hiện tại
    private String logoURL;
    private String coverImageURL;
    
    // Files upload mới (nếu có)
    private MultipartFile logoFile;
    private MultipartFile coverImageFile;
    
    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ không được quá 500 ký tự")
    private String address;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ (phải là 10 số, bắt đầu bằng 0)")
    private String phoneNumber;
    
    private Byte status;
    private String statusText;
    
    // Read-only field (chỉ hiển thị, không cho phép sửa)
    private BigDecimal commissionRate;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Thông tin user chủ shop
    private String ownerName;
    private String ownerEmail;
}