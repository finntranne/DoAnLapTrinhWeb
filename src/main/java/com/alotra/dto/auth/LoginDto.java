package com.alotra.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {

    @NotBlank(message = "Tên đăng nhập hoặc email không được để trống")
    @Size(min = 3, max = 100, message = "Tên đăng nhập hoặc email phải từ 3–100 ký tự")
    private String usernameOrEmail;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;
}
