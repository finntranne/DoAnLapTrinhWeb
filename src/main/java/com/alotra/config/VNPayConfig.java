package com.alotra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * VNPayConfig - Spring Configuration Properties Holder
 * Quản lý cấu hình VNPay từ application.properties
 * Chỉ chứa properties, logic được xử lý bởi VNPaySDK
 */
@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VNPayConfig {

    // Các thuộc tính lấy từ application.properties
    private String tmnCode;         // Mã TMN từ VNPay (UE7VL5EI)
    private String hashSecret;      // Hash secret từ VNPay (VZ4WXT2UEBZRYKHP2TI9CW5V4HNSNGVO)
    private String apiUrl;          // URL API VNPay (https://sandbox.vnpayment.vn/paymentv2/vpcpay.html)
    private String returnUrl;       // URL trả về sau thanh toán
    private String locale;          // Ngôn ngữ (vn/en)

    // Hằng số VNPay
    public static final String VNP_VERSION = "vnp_Version";
    public static final String VNP_COMMAND = "vnp_Command";
    public static final String VNP_TMNCODE = "vnp_TmnCode";
    public static final String VNP_AMOUNT = "vnp_Amount";
    public static final String VNP_CREATE_DATE = "vnp_CreateDate";
    public static final String VNP_CURR_CODE = "vnp_CurrCode";
    public static final String VNP_IPADDR = "vnp_IpAddr";
    public static final String VNP_LOCALE = "vnp_Locale";
    public static final String VNP_ORDER_INFO = "vnp_OrderInfo";
    public static final String VNP_ORDER_TYPE = "vnp_OrderType";
    public static final String VNP_RETURN_URL = "vnp_ReturnUrl";
    public static final String VNP_TXNREF = "vnp_TxnRef";
    public static final String VNP_SECURE_HASH = "vnp_SecureHash";
    public static final String VNP_EXPIRE_DATE = "vnp_ExpireDate";
    public static final String VNP_RESPONSE_CODE = "vnp_ResponseCode";
    public static final String VNP_TRANSACTION_NO = "vnp_TransactionNo";
    public static final String VNP_BANK_CODE = "vnp_BankCode";
    public static final String VNP_PAY_DATE = "vnp_PayDate";
}