package com.alotra.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RegisterUserModel {

    private String username;
    
    private String email;
    
    private String password;
    
    private String fullname;
    
    private boolean is_verified;
}
