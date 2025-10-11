package com.alotra.model;

import lombok.Data;

@Data
public class ForgotPasswordOtpDto {

	private String email;
    private String otp;
}
