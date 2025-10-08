package com.alotra.model;

import lombok.Data;

@Data
public class SignUpDto {
    private String username;
    private String email;
    private String password;
    private String fullname;
    private boolean is_verified;
}
