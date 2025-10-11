package com.alotra.service.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Mã OTP Xác Thực Tài Khoản AloTra");
            message.setText(
                "Xin chào,\n\n" +
                "Bạn hoặc ai đó vừa yêu cầu đăng ký tài khoản trên hệ thống AloTra của chúng tôi.\n\n" +
                " Mã OTP của bạn là: " + otp + "\n\n" +
                "Vui lòng nhập mã này trong vòng 60 giây để xác thực tài khoản của bạn.\n" +
                "Mã này chỉ sử dụng một lần và tuyệt đối không chia sẻ với bất kỳ ai để đảm bảo an toàn.\n\n" +
                "Nếu bạn không yêu cầu mã OTP này, hãy bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ AloTra"
            );
            message.setFrom("your-email@gmail.com");
            mailSender.send(message);
            logger.info("OTP sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Error sending OTP to: {}", toEmail, e);
        }
    }

    @Async
    public void sendPasswordResetOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Mã OTP Đặt Lại Mật Khẩu AloTra");
            message.setText(
                "Xin chào,\n\n" +
                "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản AloTra.\n\n" +
                "Mã OTP của bạn là: " + otp + "\n\n" +
                "Vui lòng nhập mã này trong vòng 60 giây.\n" +
                "Mã này chỉ sử dụng một lần.\n\n" +
                "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ AloTra"
            );
            message.setFrom("your-email@gmail.com");
            mailSender.send(message);
            logger.info("Password reset OTP sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Error sending password reset OTP to: {}", toEmail, e);
        }
    }
}