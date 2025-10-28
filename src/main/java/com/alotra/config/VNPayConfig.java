package com.alotra.config;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

// Khuyến nghị sử dụng @ConfigurationProperties nếu bạn lưu các giá trị này trong application.properties
// Ví dụ: @Value("${vnpay.tmnCode}")

public class VNPayConfig {
    
    public static final String VNP_VERSION = "2.1.0";
    public static final String VNP_COMMAND = "pay";
    public static final String VNP_CURRENCY_CODE = "VND";
    public static final String VNP_LOCALE = "vn";

    // Thay thế bằng các giá trị thực tế của bạn
    public static final String VNP_TMN_CODE = "4NI19HF0"; // Mã Terminal
    public static final String VNP_HASH_SECRET = "ZGXD52K58OCC4GQB2BDFQ0XO9JTLG2UM"; // Chuỗi bí mật
    public static final String VNP_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"; // URL thanh toán VNPay
    public static final String VNP_RETURN_URL = "http://localhost:8080/vnpay_return"; // URL nhận kết quả trả về của bạn

    // Hàm tiện ích lấy thời gian (Thường dùng cho VNP_CreateDate và VNP_ExpireDate)
    public static String getVnpayDateFormat(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return formatter.format(date);
    }
    
    // Hàm tiện ích tạo ngày hết hạn (30 phút sau)
    public static String getVnpayExpireDate() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        c.add(Calendar.MINUTE, 30);
        return getVnpayDateFormat(c.getTime());
    }
}