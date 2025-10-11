package com.alotra.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpDto {
    private String username;
    private String email;
    private String password;
    private String fullname;
    private boolean is_verified;
}