package com.alotra.controller.auth;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.service.user.PasswordResetTokenService; // ✅ IMPORT SERVICE

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class LoginController {

	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

	@Autowired
	private PasswordResetTokenService resetTokenService; // ✅ INJECT SERVICE

	@GetMapping("/login")
	public String showLoginPage(Authentication authentication) {
		if (authentication != null && authentication.isAuthenticated()
				&& !authentication.getPrincipal().equals("anonymousUser")) {
			String redirectUrl = determineTargetUrl(authentication);
			return "redirect:" + redirectUrl;
		}
		return "auth/login";
	}

	@GetMapping("/reset-password")
	public String showResetPasswordPage(@RequestParam("token") String token, Model model,
			RedirectAttributes redirectAttributes) {

		logger.info("🔗 Reset password request with token: {}", token);

		// ✅ SỬ DỤNG TOKEN SERVICE ĐỂ VALIDATE VÀ TRÍCH XUẤT EMAIL
		String email = resetTokenService.validateAndExtractEmail(token);

		if (email == null) {
			logger.warn("❌ Invalid or expired token");
			redirectAttributes.addFlashAttribute("errorMessage",
					"Đường link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu link mới.");
			return "redirect:/login";
		}

		// Token hợp lệ - Mask email và truyền vào view
		String maskedEmail = maskEmail(email);

		logger.info("✅ Valid token for user: {}", maskedEmail);

		model.addAttribute("token", token);
		model.addAttribute("maskedEmail", maskedEmail);
		model.addAttribute("fullEmail", email); // Để hiển thị tooltip nếu cần

		return "auth/reset-password";
	}

	@GetMapping("/auth/403")
	public String accessDenied() {
		return "auth/403";
	}

	/**
	 * Mask email để bảo mật Ví dụ: - nguyentrilam@gmail.com -> n***m@gmail.com -
	 * abc@example.com -> a***c@example.com - xy@test.com -> x***y@test.com
	 */
	private String maskEmail(String email) {
		if (email == null || !email.contains("@")) {
			return email;
		}

		String[] parts = email.split("@");
		String localPart = parts[0]; // Phần trước @
		String domain = parts[1]; // Phần sau @

		// Nếu localPart quá ngắn (1-2 ký tự)
		if (localPart.length() <= 2) {
			return localPart.charAt(0) + "***@" + domain;
		}

		// Lấy ký tự đầu và cuối, giữa thay bằng ***
		char firstChar = localPart.charAt(0);
		char lastChar = localPart.charAt(localPart.length() - 1);

		return firstChar + "***" + lastChar + "@" + domain;
	}

	private String determineTargetUrl(Authentication authentication) {
		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

		if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
			return "/admin/dashboard";
		}
		if (authorities.stream().anyMatch(a -> a.getAuthority().equals("VENDOR"))) {
			return "/vendor/dashboard";
		}
		if (authorities.stream().anyMatch(a -> a.getAuthority().equals("CUSTOMER"))) {
			return "/";
		}

		return "/";
	}
}