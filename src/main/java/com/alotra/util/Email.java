package com.alotra.util;

import java.util.Properties;
import java.util.Random;

import com.alotra.entity.user.User;


import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;



public class Email {

	public String getRandom() {
		Random rnd = new Random();
		int number = rnd.nextInt(999999);
		return String.format("%06d", number);		
	}

	public static boolean sendEmail(User user) {
		boolean test = false;
		String toEmail = user.getEmail();
		String fromEmail = "nguyentrilam0304@gmail.com";
		String password = "itsp ryoi agwh qyuq";
		
		try {
			Properties pr = configEmail(new Properties());
			Session session = Session.getInstance(pr, new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(fromEmail, password);
				}
			});
			Message mess = new MimeMessage(session);
			
			mess.setHeader("Content-Type", "text/plain; charset=UTF-8");

			mess.setFrom(new InternetAddress(fromEmail));

			mess.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

			mess.setSubject("Confirm Code");
			
			mess.setText("Your code is: " + user.getOtpCode());
			
			Transport.send(mess);
			
			test = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return test;
	}

	public static boolean sendPasswordResetLink(String toEmail, String token, String originUrl) {
		boolean test = false;
		String fromEmail = "nguyentrilam0304@gmail.com";
		String password = "itsp ryoi agwh qyuq";
		
		try {
			Properties pr = configEmail(new Properties());
			Session session = Session.getInstance(pr, new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(fromEmail, password);
				}
			});
			Message mess = new MimeMessage(session);
			
			mess.setHeader("Content-Type", "text/html; charset=UTF-8"); // Sửa thành text/html
			mess.setFrom(new InternetAddress(fromEmail));
			mess.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
			mess.setSubject("Yêu cầu đặt lại mật khẩu Alotra"); // Sửa Subject
			
            // Tạo nội dung HTML cho email
            String resetLink = originUrl + "/reset-password?token=" + token;
            String emailContent = "<html><body>"
                                + "<p>Xin chào,</p>"
                                + "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>"
                                + "<p>Vui lòng nhấp vào đường link dưới đây để đặt lại mật khẩu:</p>"
                                + "<p><a href=\"" + resetLink + "\"><b>ĐẶT LẠI MẬT KHẨU</b></a></p>"
                                + "<p>Link này sẽ hết hạn sau 15 phút.</p>"
                                + "<p>Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>"
                                + "<p>Trân trọng,<br>Đội ngũ Alotra</p>"
                                + "</body></html>";
			
			mess.setContent(emailContent, "text/html; charset=UTF-8"); // Set nội dung là HTML
			
			Transport.send(mess);
			test = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return test;
	}
	
	
	private static Properties configEmail(Properties pr) {
		pr.setProperty("mail.smtp.host", "smtp.gmail.com");
		pr.setProperty("mail.smtp.port", "587");
		pr.setProperty("mail.smtp.auth", "true");
		pr.setProperty("mail.smtp.starttls.enable", "true");
		pr.put("mail.smtp.socketFactory.port", "587");
		pr.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		return pr;
	}
}
