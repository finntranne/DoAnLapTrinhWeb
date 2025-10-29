package com.alotra.dto.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopEmployeeDTO {
    
    private Integer employeeId;
    
    private Integer userId;
    
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    @NotBlank(message = "Email không được để trống")
    private String email;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;
    
    private String avatarURL;
    
    @NotNull(message = "Vui lòng chọn vai trò")
    private Integer roleId; // 4: SHIPPER, 5: STAFF
    
    private String roleName;
    
    private String status; // Active, Inactive
    
    private LocalDateTime assignedAt;
    
    private LocalDateTime updatedAt;
}