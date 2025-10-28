package com.alotra.controller.auth;

import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.dto.auth.*;
import com.alotra.repository.user.RoleRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.security.MyUserDetails;
import com.alotra.security.jwt.JwtTokenProvider;
import com.alotra.service.user.EmailService;
import com.alotra.service.user.PasswordResetTokenService; // ✅ IMPORT MỚI

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EmailService emailService;

	@Autowired
	private JwtTokenProvider tokenProvider;

	@Autowired
	private PasswordResetTokenService resetTokenService; // ✅ INJECT SERVICE MỚI

	private String generateOtp() {
		return String.format("%06d", new Random().nextInt(999999));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpDto signUpDto, BindingResult result) {
		logger.info("Signup request for username: {}", signUpDto.getUsername());

		if (result.hasErrors()) {
			Map<String, String> errors = new HashMap<>();
			result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "VALIDATION_ERROR", "message", "Dữ liệu không hợp lệ", "errors", errors));
		}

		// Normalize username và email
		String normalizedUsername = signUpDto.getUsername().toLowerCase().trim();
		String normalizedEmail = signUpDto.getEmail().toLowerCase().trim();

		// ===== KIỂM TRA USERNAME =====
		Optional<User> existingUserByUsername = userRepository.findByUsername(normalizedUsername);
		if (existingUserByUsername.isPresent()) {
			User existingUser = existingUserByUsername.get();

			// Nếu tài khoản đã được xác thực (status = 1)
			if (existingUser.getStatus() == 1) {
				return ResponseEntity.badRequest()
						.body(Map.of("errorCode", "USERNAME_TAKEN", "message", "Tên đăng nhập đã tồn tại."));
			}

			// Nếu tài khoản chưa xác thực (status = 0)
			if (existingUser.getStatus() == 0) {
				// Kiểm tra OTP còn hạn hay không
				if (existingUser.getOtpExpiryTime() != null
						&& LocalDateTime.now().isBefore(existingUser.getOtpExpiryTime())) {
					// OTP còn hạn - cho phép truy cập trang OTP
					logger.info("User exists with valid OTP, redirecting to OTP verification: {}", normalizedUsername);

					// Tính thời gian còn lại của OTP
					long secondsRemaining = java.time.Duration
							.between(LocalDateTime.now(), existingUser.getOtpExpiryTime()).getSeconds();

					return ResponseEntity.ok(Map.of("message", "Tài khoản đã được tạo, vui lòng xác thực OTP", "email",
							existingUser.getEmail(), "redirectToOtp", true, "otpTimeRemaining", secondsRemaining));
				} else {
					// OTP hết hạn - xóa tài khoản cũ để đăng ký lại
					logger.info("Deleting expired unverified account: {}", normalizedUsername);
					userRepository.delete(existingUser);
				}
			}
		}

		// ===== KIỂM TRA EMAIL =====
		Optional<User> existingUserByEmail = userRepository.findByEmail(normalizedEmail);
		if (existingUserByEmail.isPresent()) {
			User existingUser = existingUserByEmail.get();

			// Nếu tài khoản đã được xác thực
			if (existingUser.getStatus() == 1) {
				return ResponseEntity.badRequest()
						.body(Map.of("errorCode", "EMAIL_TAKEN", "message", "Email đã được sử dụng."));
			}

			// Nếu tài khoản chưa xác thực
			if (existingUser.getStatus() == 0) {
				// Kiểm tra OTP còn hạn hay không
				if (existingUser.getOtpExpiryTime() != null
						&& LocalDateTime.now().isBefore(existingUser.getOtpExpiryTime())) {
					// OTP còn hạn - cho phép truy cập trang OTP
					logger.info("User exists with valid OTP, redirecting to OTP verification: {}", normalizedEmail);

					// Tính thời gian còn lại của OTP
					long secondsRemaining = java.time.Duration
							.between(LocalDateTime.now(), existingUser.getOtpExpiryTime()).getSeconds();

					return ResponseEntity.ok(Map.of("message", "Tài khoản đã được tạo, vui lòng xác thực OTP", "email",
							existingUser.getEmail(), "redirectToOtp", true, "otpTimeRemaining", secondsRemaining));
				} else {
					// OTP hết hạn - xóa tài khoản cũ để đăng ký lại
					logger.info("Deleting expired unverified account with email: {}", normalizedEmail);
					userRepository.delete(existingUser);
				}
			}
		}

		// ===== KIỂM TRA SỐ ĐIỆN THOẠI =====
		if (signUpDto.getPhoneNumber() != null && !signUpDto.getPhoneNumber().trim().isEmpty()) {
			String phoneNumber = signUpDto.getPhoneNumber().trim();

			Optional<User> existingUserByPhone = userRepository.findByPhoneNumber(phoneNumber);
			if (existingUserByPhone.isPresent()) {
				User existingUser = existingUserByPhone.get();

				// Nếu tài khoản đã được xác thực
				if (existingUser.getStatus() == 1) {
					return ResponseEntity.badRequest()
							.body(Map.of("errorCode", "PHONE_TAKEN", "message", "Số điện thoại đã được sử dụng."));
				}

				// Nếu tài khoản chưa xác thực và OTP hết hạn
				if (existingUser.getStatus() == 0 && (existingUser.getOtpExpiryTime() == null
						|| LocalDateTime.now().isAfter(existingUser.getOtpExpiryTime()))) {
					logger.info("Deleting expired unverified account with phone: {}", phoneNumber);
					userRepository.delete(existingUser);
				}
			}

			signUpDto.setPhoneNumber(phoneNumber);
		} else {
			signUpDto.setPhoneNumber(null);
		}

		// ===== TẠO TÀI KHOẢN MỚI =====
		try {
			User user = new User();
			user.setUsername(normalizedUsername);
			user.setEmail(normalizedEmail);
			user.setPhoneNumber(signUpDto.getPhoneNumber());
			user.setFullName(signUpDto.getFullname());
			user.setPassword(passwordEncoder.encode(signUpDto.getPassword()));
			user.setStatus((byte) 0);

			String otp = generateOtp();
			user.setOtpCode(otp);
			user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
			user.setOtpPurpose("REGISTER");

			Role customerRole = roleRepository.findByRoleName("CUSTOMER")
					.orElseThrow(() -> new RuntimeException("Customer role not found"));
			user.setRoles(new HashSet<>(Collections.singletonList(customerRole)));

			user = userRepository.save(user);

			try {
				emailService.sendOtp(user.getEmail(), otp);
			} catch (Exception e) {
				logger.error("Failed to send OTP email", e);
			}

			logger.info("User registered: {}", normalizedUsername);
			return ResponseEntity.ok(Map.of("message", "OTP đã được gửi tới email", "email", user.getEmail(),
					"redirectToOtp", true, "otpTimeRemaining", 300 // 5 phút = 300 giây
			));

		} catch (DataIntegrityViolationException e) {
			logger.error("Data integrity violation during signup", e);

			String errorMessage = "Lỗi dữ liệu. Vui lòng kiểm tra lại thông tin.";
			if (e.getMessage().contains("Username")) {
				errorMessage = "Tên đăng nhập đã tồn tại.";
			} else if (e.getMessage().contains("Email")) {
				errorMessage = "Email đã được sử dụng.";
			} else if (e.getMessage().contains("PhoneNumber")) {
				errorMessage = "Số điện thoại đã được sử dụng.";
			}

			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "DATA_INTEGRITY_ERROR", "message", errorMessage));
		} catch (Exception e) {
			logger.error("Signup error", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("errorCode", "SERVER_ERROR", "message", "Lỗi hệ thống. Vui lòng thử lại sau."));
		}
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerificationDto dto, BindingResult result) {
		logger.info("Verifying OTP for: {}", dto.getEmail());

		if (result.hasErrors()) {
			Map<String, String> errors = new HashMap<>();
			result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "VALIDATION_ERROR", "message", "Dữ liệu không hợp lệ", "errors", errors));
		}

		Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
		if (userOpt.isEmpty()) {
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "USER_NOT_FOUND", "message", "Người dùng không tồn tại!"));
		}

		User user = userOpt.get();

		if (user.getOtpCode() == null || !user.getOtpCode().equals(dto.getOtp())) {
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "INVALID_OTP", "message", "Mã OTP không hợp lệ!"));
		}

		if (user.getOtpExpiryTime() != null && LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "OTP_EXPIRED", "message", "Mã OTP đã hết hạn!"));
		}

		user.setStatus((byte) 1);
		user.setOtpCode(null);
		user.setOtpExpiryTime(null);
		user.setOtpPurpose(null);
		userRepository.save(user);

		logger.info("User verified: {}", user.getEmail());
		return ResponseEntity.ok(Map.of("message", "Tài khoản đã được xác thực! Bạn có thể đăng nhập."));
	}

	@PostMapping("/resend-otp")
	public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
		String email = request.get("email");
		logger.info("Resend OTP for: {}", email);

		Optional<User> userOpt = userRepository.findByEmail(email);
		if (userOpt.isEmpty()) {
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "USER_NOT_FOUND", "message", "Người dùng không tồn tại!"));
		}

		User user = userOpt.get();
		String otp = generateOtp();
		user.setOtpCode(otp);
		user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
		userRepository.save(user);

		try {
			emailService.sendOtp(user.getEmail(), otp);
		} catch (Exception e) {
			logger.error("Failed to resend OTP email", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("errorCode", "EMAIL_ERROR", "message", "Không thể gửi OTP. Vui lòng thử lại sau."));
		}

		return ResponseEntity.ok(Map.of("message", "OTP đã được gửi lại"));
	}

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginDto loginDto, BindingResult result,
			HttpServletRequest request, HttpServletResponse response) {

		logger.info("Login attempt: {}", loginDto.getUsernameOrEmail());

		if (result.hasErrors()) {
			Map<String, String> errors = new HashMap<>();
			result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "VALIDATION_ERROR", "message", "Dữ liệu không hợp lệ", "errors", errors));
		}

		try {
			Optional<User> userOpt = userRepository.findByUsernameOrEmail(loginDto.getUsernameOrEmail(),
					loginDto.getUsernameOrEmail());

			if (userOpt.isEmpty()) {
				return ResponseEntity.badRequest().body(
						Map.of("errorCode", "USER_NOT_FOUND", "message", "Tên đăng nhập hoặc email không tồn tại!"));
			}

			User user = userOpt.get();

			if (user.getStatus() == 0) {
				return ResponseEntity.badRequest().body(Map.of("errorCode", "USER_NOT_VERIFIED", "message",
						"Tài khoản chưa được xác thực!", "email", user.getEmail()));
			}

			if (user.getStatus() == 2) {
				return ResponseEntity.badRequest()
						.body(Map.of("errorCode", "USER_SUSPENDED", "message", "Tài khoản đã bị tạm khóa!"));
			}

			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginDto.getUsernameOrEmail(), loginDto.getPassword()));

			SecurityContextHolder.getContext().setAuthentication(authentication);

			user.setLastLoginAt(LocalDateTime.now());
			userRepository.save(user);

			String jwt = tokenProvider.generateJwtToken(authentication);

			Cookie jwtCookie = new Cookie("jwtToken", jwt);
			jwtCookie.setHttpOnly(true);
			jwtCookie.setPath("/");
			jwtCookie.setMaxAge(tokenProvider.getJwtExpirationMs() / 1000);
			jwtCookie.setSecure(false);

			response.addCookie(jwtCookie);

			String redirectUrl = determineTargetUrl(authentication);

			MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
			List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
					.collect(Collectors.toList());

			logger.info("User logged in via API: {}, setting JWT cookie. Redirecting to: {}", userDetails.getUsername(),
					redirectUrl);

			return ResponseEntity
					.ok(Map.of("message", "Đăng nhập thành công!", "username", userDetails.getUser().getUsername(),
							"email", userDetails.getUsername(), "roles", roles, "redirectUrl", redirectUrl));

		} catch (Exception e) {
			logger.error("Login error", e);
			return ResponseEntity.badRequest().body(Map.of("errorCode", "INVALID_CREDENTIALS", "message",
					"Tên đăng nhập hoặc mật khẩu không chính xác!"));
		}
	}

	// ✅ CẬP NHẬT: SỬ DỤNG CUSTOM TOKEN SERVICE
	@PostMapping("/forgot-password")
	public ResponseEntity<?> sendForgotPasswordLink(@Valid @RequestBody ForgotPasswordRequestDto dto,
			BindingResult result, HttpServletRequest request) {

		logger.info("Forgot password request: {}", dto.getEmail());

		if (result.hasErrors()) {
			Map<String, String> errors = new HashMap<>();
			result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "VALIDATION_ERROR", "message", "Dữ liệu không hợp lệ", "errors", errors));
		}

		Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
		if (userOpt.isEmpty()) {
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "USER_NOT_FOUND", "message", "Email không tồn tại!"));
		}

		User user = userOpt.get();

		// ✅ KIỂM TRA TRẠNG THÁI TÀI KHOẢN
		if (user.getStatus() == 0) {
			// Tài khoản chưa xác thực
			// Kiểm tra xem OTP còn hạn không
			if (user.getOtpExpiryTime() != null && LocalDateTime.now().isBefore(user.getOtpExpiryTime())) {
				// OTP còn hạn - hướng dẫn người dùng xác thực OTP trước
				long secondsRemaining = java.time.Duration.between(LocalDateTime.now(), user.getOtpExpiryTime())
						.getSeconds();

				return ResponseEntity.badRequest().body(Map.of("errorCode", "ACCOUNT_NOT_VERIFIED", "message",
						"Tài khoản chưa được xác thực. Vui lòng xác thực OTP trước khi đặt lại mật khẩu.", "email",
						user.getEmail(), "needVerification", true, "otpTimeRemaining", secondsRemaining));
			} else {
				// OTP hết hạn - yêu cầu đăng ký lại
				return ResponseEntity.badRequest().body(Map.of("errorCode", "ACCOUNT_NOT_VERIFIED_EXPIRED", "message",
						"Tài khoản chưa được xác thực và OTP đã hết hạn. Vui lòng đăng ký lại."));
			}
		}

		if (user.getStatus() == 2) {
			// Tài khoản bị khóa
			return ResponseEntity.badRequest().body(Map.of("errorCode", "ACCOUNT_SUSPENDED", "message",
					"Tài khoản đã bị tạm khóa. Vui lòng liên hệ quản trị viên."));
		}

		// ✅ SỬ DỤNG CUSTOM TOKEN SERVICE (không lưu vào database)
		String token = resetTokenService.generateToken(user.getEmail(), 15); // 15 phút

		logger.info("Generated custom token for {}: {}", user.getEmail(), token);

		try {
			String originUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
			emailService.sendPasswordResetLink(user.getEmail(), token, originUrl);

			logger.info("Password reset link sent to: {}", user.getEmail());

		} catch (Exception e) {
			logger.error("Failed to send password reset link", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("errorCode", "EMAIL_ERROR", "message", "Không thể gửi email. Vui lòng thử lại sau."));
		}

		return ResponseEntity.ok(Map.of("message",
				"Link đặt lại mật khẩu đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư (kể cả thư spam)."));
	}

	// ✅ CẬP NHẬT: VALIDATE TOKEN BẰNG SERVICE
	@PostMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDto dto, BindingResult result) {

		if (result.hasErrors()) {
			Map<String, String> errors = new HashMap<>();
			result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "VALIDATION_ERROR", "message", "Dữ liệu không hợp lệ", "errors", errors));
		}

		if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "PASSWORD_MISMATCH", "message", "Mật khẩu xác nhận không khớp!"));
		}

		// ✅ VALIDATE TOKEN VÀ TRÍCH XUẤT EMAIL
		String email = resetTokenService.validateAndExtractEmail(dto.getToken());

		if (email == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "INVALID_TOKEN", "message", "Token không hợp lệ hoặc đã hết hạn!"));
		}

		logger.info("Valid token for email: {}", email);

		Optional<User> userOpt = userRepository.findByEmail(email);
		if (userOpt.isEmpty()) {
			return ResponseEntity.badRequest()
					.body(Map.of("errorCode", "USER_NOT_FOUND", "message", "Người dùng không tồn tại!"));
		}

		User user = userOpt.get();

		// Đặt lại mật khẩu
		user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
		userRepository.save(user);

		logger.info("Password reset successfully for: {}", user.getEmail());
		return ResponseEntity.ok(Map.of("message", "Mật khẩu đã được đặt lại thành công!"));
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
		logger.info("Logout request");

		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			if (authentication != null) {
				logger.info("User logging out: {}", authentication.getName());

				SecurityContextHolder.clearContext();

				HttpSession session = request.getSession(false);
				if (session != null) {
					session.invalidate();
				}

				Cookie cookie = new Cookie("JSESSIONID", null);
				cookie.setPath("/");
				cookie.setMaxAge(0);
				response.addCookie(cookie);
			}

			Cookie jwtCookie = new Cookie("jwtToken", null);
			jwtCookie.setPath("/");
			jwtCookie.setMaxAge(0);
			response.addCookie(jwtCookie);

			return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công!"));
		} catch (Exception e) {
			logger.error("Logout error", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("errorCode", "LOGOUT_ERROR", "message", "Lỗi khi đăng xuất. Vui lòng thử lại."));
		}
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