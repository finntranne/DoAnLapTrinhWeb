//package com.alotra.service.checkout;
//
//import com.alotra.config.VNPayConfig;
//import com.alotra.util.VNPayUtil;
//
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.stereotype.Service;
//
//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
//@Service
//public class VNPayService {
//
//    public String createOrder(int orderId, double totalAmount, HttpServletRequest request) throws UnsupportedEncodingException {
//        String orderType = "other";
//
//        Map<String, String> vnp_Params = new HashMap<>();
//        vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
//        vnp_Params.put("vnp_Command", VNPayConfig.vnp_Command);
//        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
//        vnp_Params.put("vnp_Amount", String.valueOf((long) (totalAmount * 100))); // VNPay tính theo đơn vị đồng
//        vnp_Params.put("vnp_CurrCode", "VND");
//
//        String bankCode = request.getParameter("bankCode");
//        if (bankCode != null && !bankCode.isEmpty()) {
//            vnp_Params.put("vnp_BankCode", bankCode);
//        }
//
//        vnp_Params.put("vnp_TxnRef", String.valueOf(orderId));
//        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang #" + orderId);
//        vnp_Params.put("vnp_OrderType", orderType);
//        vnp_Params.put("vnp_Locale", "vn");
//        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
//        vnp_Params.put("vnp_IpAddr", getIpAddress(request));
//
//        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//        String vnp_CreateDate = formatter.format(cld.getTime());
//        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
//
//        cld.add(Calendar.MINUTE, 15);
//        String vnp_ExpireDate = formatter.format(cld.getTime());
//        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
//
//        // ---- Tạo chuỗi hash data ----
//        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
//        Collections.sort(fieldNames);
//        StringBuilder hashData = new StringBuilder();
//        StringBuilder query = new StringBuilder();
//        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
//            String fieldName = itr.next();
//            String fieldValue = vnp_Params.get(fieldName);
//            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
//                hashData.append(fieldName).append('=')
//                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
//                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
//                        .append('=')
//                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
//                if (itr.hasNext()) {
//                    hashData.append('&');
//                    query.append('&');
//                }
//            }
//        }
//
//        // ---- Sinh checksum ----
//        String vnp_SecureHash = VNPayUtil.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
//        query.append("&vnp_SecureHash=").append(vnp_SecureHash);
//
//        return VNPayConfig.vnp_Url + "?" + query.toString();
//    }
//
//    private String getIpAddress(HttpServletRequest request) {
//        String ipAddress = request.getHeader("X-FORWARDED-FOR");
//        if (ipAddress == null) {
//            ipAddress = request.getRemoteAddr();
//        }
//        return ipAddress;
//    }
//}
