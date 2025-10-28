package com.alotra.service.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.internet.MimeMessage;

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
			message.setText("Xin chào,\n\n"
					+ "Bạn hoặc ai đó vừa yêu cầu đăng ký tài khoản trên hệ thống AloTra của chúng tôi.\n\n"
					+ " Mã OTP của bạn là: " + otp + "\n\n"
					+ "Vui lòng nhập mã này trong vòng 5 phút để xác thực tài khoản của bạn.\n"
					+ "Mã này chỉ sử dụng một lần và tuyệt đối không chia sẻ với bất kỳ ai để đảm bảo an toàn.\n\n"
					+ "Nếu bạn không yêu cầu mã OTP này, hãy bỏ qua email này.\n\n" + "Trân trọng,\n"
					+ "Đội ngũ AloTra");
			message.setFrom("nguyentrilam0304@gmail.com");

			mailSender.send(message);
			logger.info("✅ OTP sent successfully to: {}", toEmail);

		} catch (Exception e) {
			logger.error("❌ Error sending OTP to: {}", toEmail, e);
			throw new RuntimeException("Không thể gửi email OTP", e);
		}
	}

	@Async
	public void sendPasswordResetLink(String toEmail, String token, String originUrl) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

			String resetLink = originUrl + "/reset-password?token=" + token;

			logger.info("🔗 Generating reset link: {}", resetLink);

			String emailContent = "<!DOCTYPE html>" + "<html>" + "<head>" + "    <meta charset='UTF-8'>" + "    <style>"
					+ "        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }"
					+ "        .container { max-width: 600px; margin: 20px auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }"
					+ "        .header { text-align: center; margin-bottom: 30px; background: linear-gradient(135deg, #2da0a8 0%, #1e7d85 100%); padding: 20px; border-radius: 10px 10px 0 0; margin: -30px -30px 30px -30px; }"
					+ "        .header h1 { color: white; margin: 0; font-size: 24px; }"
					+ "        .content { color: #555; line-height: 1.8; }"
					+ "        .button-container { text-align: center; margin: 30px 0; }"
					+ "        .button { display: inline-block; padding: 15px 40px; background: linear-gradient(135deg, #2da0a8 0%, #1e7d85 100%); color: white !important; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 6px rgba(45, 160, 168, 0.3); transition: all 0.3s; }"
					+ "        .button:hover { transform: translateY(-2px); box-shadow: 0 6px 12px rgba(45, 160, 168, 0.4); }"
					+ "        .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 5px; }"
					+ "        .warning-title { color: #856404; font-weight: bold; margin-bottom: 10px; display: flex; align-items: center; }"
					+ "        .warning-icon { font-size: 20px; margin-right: 8px; }"
					+ "        .warning ul { margin: 10px 0 0 20px; color: #856404; }"
					+ "        .info-box { background-color: #e7f3ff; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 5px; }"
					+ "        .link-text { word-break: break-all; color: #2da0a8; background: #f8f9fa; padding: 10px; border-radius: 5px; font-size: 13px; }"
					+ "        .footer { text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; color: #999; font-size: 12px; }"
					+ "        .footer p { margin: 5px 0; }" + "    </style>" + "</head>" + "<body>"
					+ "    <div class='container'>" + "        <div class='header'>"
					+ "            <h1>🔐 Đặt Lại Mật Khẩu</h1>" + "        </div>" + "        <div class='content'>"
					+ "            <p style='font-size: 16px;'>Xin chào,</p>"
					+ "            <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản <strong>AloTra</strong> của bạn với email:</p>"
					+ "            <div class='info-box'>" + "                <strong style='color: #2196F3;'>📧 "
					+ toEmail + "</strong>" + "            </div>"
					+ "            <p>Để tiếp tục quá trình đặt lại mật khẩu, vui lòng nhấn vào nút bên dưới:</p>"
					+ "            <div class='button-container'>" + "                <a href='" + resetLink
					+ "' class='button'>ĐẶT LẠI MẬT KHẨU</a>" + "            </div>"
					+ "            <div class='warning'>" + "                <div class='warning-title'>"
					+ "                    <span class='warning-icon'>⚠️</span>"
					+ "                    <span>Lưu ý quan trọng:</span>" + "                </div>"
					+ "                <ul style='margin: 10px 0;'>"
					+ "                    <li>Link này chỉ có hiệu lực trong <strong>15 phút</strong></li>"
					+ "                    <li>Link chỉ có thể sử dụng <strong>một lần duy nhất</strong></li>"
					+ "                    <li>Nếu bạn <strong>không yêu cầu</strong> đặt lại mật khẩu, vui lòng <strong>bỏ qua</strong> email này và không chia sẻ link với bất kỳ ai</li>"
					+ "                </ul>" + "            </div>"
					+ "            <p style='margin-top: 20px; font-size: 14px;'>Hoặc bạn có thể <strong>copy</strong> và <strong>dán</strong> đường link sau vào trình duyệt:</p>"
					+ "            <div class='link-text'>" + resetLink + "</div>"
					+ "            <p style='margin-top: 30px; color: #999; font-size: 13px; font-style: italic;'>"
					+ "                💡 Nếu bạn gặp vấn đề với nút bên trên, hãy sử dụng đường link phía trên."
					+ "            </p>" + "        </div>" + "        <div class='footer'>"
					+ "            <p><strong>Email này được gửi tự động</strong></p>"
					+ "            <p>Vui lòng không trả lời email này</p>"
					+ "            <p style='margin-top: 15px;'>&copy; 2025 <strong>AloTra</strong>. All rights reserved.</p>"
					+ "        </div>" + "    </div>" + "</body>" + "</html>";

			helper.setText(emailContent, true);
			helper.setTo(toEmail);
			helper.setSubject("🔒 Yêu cầu đặt lại mật khẩu - AloTra");
			helper.setFrom("nguyentrilam0304@gmail.com");

			mailSender.send(mimeMessage);
			logger.info("✅ Password reset link sent successfully to: {}", toEmail);

		} catch (Exception e) {
			logger.error("❌ Error sending password reset link to: {}", toEmail, e);
			throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.", e);
		}
	}

	@Async
	public void sendPasswordResetOtp(String toEmail, String otp) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(toEmail);
			message.setSubject("Mã OTP Đặt Lại Mật Khẩu AloTra");
			message.setText("Xin chào,\n\n" + "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản AloTra.\n\n"
					+ "Mã OTP của bạn là: " + otp + "\n\n" + "Vui lòng nhập mã này trong vòng 5 phút.\n"
					+ "Mã này chỉ sử dụng một lần.\n\n"
					+ "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.\n\n" + "Trân trọng,\n"
					+ "Đội ngũ AloTra");
			message.setFrom("nguyentrilam0304@gmail.com");

			mailSender.send(message);
			logger.info("✅ Password reset OTP sent successfully to: {}", toEmail);

		} catch (Exception e) {
			logger.error("❌ Error sending password reset OTP to: {}", toEmail, e);
			throw new RuntimeException("Không thể gửi email OTP", e);
		}
	}
}