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
