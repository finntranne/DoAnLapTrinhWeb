package com.alotra.config;

import org.springframework.stereotype.Component;

@Component
public class VNPayConfig {
    // ⭐ THÔNG TIN DEMO CHÍNH THỨC TỪ VNPAY
    public static final String vnp_TmnCode = "UE7VL5EI";
    public static final String vnp_HashSecret = "VZ4WXT2UEBZRYKHP2TI9CW5V4HNSNGVO";
    
    public static final String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String vnp_ReturnUrl = "http://localhost:8080/vnpay-return";
    public static final String vnp_Version = "2.0.0";
    public static final String vnp_Command = "pay";
}