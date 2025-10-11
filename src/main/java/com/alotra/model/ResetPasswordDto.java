package com.alotra.model;

import lombok.Data;

@Data
public class ResetPasswordDto {

	private String email;
    private String newPassword;
}
