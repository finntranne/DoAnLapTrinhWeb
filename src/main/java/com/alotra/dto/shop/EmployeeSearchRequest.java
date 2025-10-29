package com.alotra.dto.shop;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSearchRequest {
    
    @NotBlank(message = "Vui lòng nhập email hoặc số điện thoại")
    private String searchTerm; // Email hoặc số điện thoại để tìm user
}