package com.alotra.service.checkout;

import com.alotra.entity.order.Order;
import com.alotra.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger; // Thêm import log
import org.slf4j.LoggerFactory; // Thêm import log
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    // Thêm logger
    private static final Logger logger = LoggerFactory.getLogger(VNPayService.class);

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String vnpayUrl;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    public String createPaymentUrl(Order order, HttpServletRequest request) throws UnsupportedEncodingException {

        long amount = order.getGrandTotal().multiply(new BigDecimal(100)).longValue();
        String vnp_TxnRef = String.valueOf(order.getOrderID());
        String vnp_IpAddr = VNPayUtil.getIpAddress(request);
        // Đảm bảo OrderInfo được encode đúng nếu có tiếng Việt
        String vnp_OrderInfo = "Thanh toan don hang ALOTRA ID: " + order.getOrderID();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", tmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        logger.info("--- VNPAY Request Parameters BEFORE Hashing ---");
        vnp_Params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> logger.info("{}: {}", entry.getKey(), entry.getValue()));

        // --- TẠO CHỮ KÝ ---
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames); // Sắp xếp tên tham số theo alphabet
        StringBuilder hashData = new StringBuilder(); // Chuỗi để tạo chữ ký (giá trị gốc)
        StringBuilder query = new StringBuilder();    // Chuỗi tham số trên URL (giá trị đã encode UTF-8)

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {

                // 1. Build hashData: Nối tên tham số, dấu '=', giá trị GỐC
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue); // <-- QUAN TRỌNG: Dùng giá trị gốc, KHÔNG encode

                // 2. Build query: Nối tên tham số (đã encode UTF-8), dấu '=', giá trị (đã encode UTF-8)
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8)); // <-- QUAN TRỌNG: UTF-8
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8)); // <-- QUAN TRỌNG: UTF-8

                // Thêm dấu '&' nếu chưa phải tham số cuối cùng
                if (itr.hasNext()) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        // Tạo chữ ký từ chuỗi hashData (giá trị gốc)
        String vnp_SecureHash = VNPayUtil.hmacSHA512(hashSecret, hashData.toString());
        // Thêm chữ ký vào cuối chuỗi query
        query.append("&vnp_SecureHash=");
        query.append(vnp_SecureHash);

        // Tạo URL hoàn chỉnh
        String paymentUrl = vnpayUrl + "?" + query.toString();

        logger.info("--- VNPAY DEBUG ---");
        logger.info("HashData      : {}", hashData.toString());
        logger.info("Calculated Hash: {}", vnp_SecureHash);
        logger.info("Payment URL   : {}", paymentUrl);
        logger.info("--- END VNPAY DEBUG ---");

        return paymentUrl;
    }
}