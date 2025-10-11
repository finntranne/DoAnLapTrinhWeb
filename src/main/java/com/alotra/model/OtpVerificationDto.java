package com.alotra.model;

import lombok.Data;

@Data
public class OtpVerificationDto {

	private String email;
    private String otp;
}
