package com.alotra.dto.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShopRegistrationDTO {

    @NotBlank(message = "Tên Shop không được để trống.")
    @Size(min = 3, max = 100, message = "Tên Shop phải từ 3 đến 100 ký tự.")
    private String shopName;

    @NotBlank(message = "Mô tả không được để trống.")
    private String description;

    @NotBlank(message = "Địa chỉ phải được cung cấp.")
    private String address;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Size(min = 10, max = 15, message = "Số điện thoại không hợp lệ.")
    private String phoneNumber;
    
}