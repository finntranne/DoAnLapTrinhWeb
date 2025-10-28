package com.alotra.config;

import org.springframework.stereotype.Component;

@Component
public class VNPayConfig {
    // ⭐ THÔNG TIN DEMO CHÍNH THỨC TỪ VNPAY
    public static final String vnp_TmnCode = "4NI19HF0";
    public static final String vnp_HashSecret = "ZGXD52K58OCC4GQB2BDFQ0XO9JTLG2UM";
    
    public static final String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String vnp_ReturnUrl = "http://localhost:8080/vnpay-return";
    public static final String vnp_Version = "2.0.0";
    public static final String vnp_Command = "pay";
}