package com.alotra.service.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

	@Autowired
    private JavaMailSender mailSender;

    public void sendOtp(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã OTP Xác Thực Tài Khoản của Bạn");
        message.setText(
            "Xin chào,\n\n" +
            "Bạn hoặc ai đó vừa yêu cầu đăng ký/tài khoản đăng nhập trên hệ thống của chúng tôi.\n\n" +
            "Mã OTP của bạn là: " + otp + "\n\n" +
            "Vui lòng nhập mã này trong vòng 60 giây để xác thực tài khoản của bạn. " +
            "Mã này chỉ sử dụng một lần và tuyệt đối không chia sẻ với bất kỳ ai để đảm bảo an toàn.\n\n" +
            "Nếu bạn không yêu cầu mã OTP này, hãy bỏ qua email này và đảm bảo rằng tài khoản của bạn được bảo mật.\n\n" +
            "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.\n\n" +
            "Trân trọng,\nĐội ngũ Hỗ trợ"
        );
        mailSender.send(message);
    }
}
