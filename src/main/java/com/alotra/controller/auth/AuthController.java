package com.alotra.controller.auth;

import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.dto.auth.*;
import com.alotra.repository.user.RoleRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.user.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

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

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignUpDto signUpDto) {
        logger.info("Signup request for username: {}", signUpDto.getUsername());

        if (userRepository.existsByUsername(signUpDto.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "USERNAME_TAKEN",
                    "message", "Tên đăng nhập đã tồn tại."
            ));
        }

        if (userRepository.existsByEmail(signUpDto.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "EMAIL_TAKEN",
                    "message", "Email đã được sử dụng."
            ));
        }

        try {
            User user = new User();
            user.setUsername(signUpDto.getUsername());
            user.setFullname(signUpDto.getFullname());
            user.setEmail(signUpDto.getEmail());
            user.setPassword(passwordEncoder.encode(signUpDto.getPassword()));
            user.setStatus((byte) 1);
            user.setVerified(false);

            String otp = generateOtp();
            user.setCodeOTP(otp);
            user.setOtpExpiryTime(LocalDateTime.now().plusSeconds(60));
            user.setOtpPurpose("REGISTER");

            Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                    .orElse(new Role(2, "CUSTOMER"));
            user.setRoles(new HashSet<>(Collections.singletonList(customerRole)));

            userRepository.save(user);
            emailService.sendOtp(user.getEmail(), otp);

            logger.info("User registered: {}", signUpDto.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "OTP đã được gửi tới email",
                    "email", user.getEmail()
            ));
        } catch (Exception e) {
            logger.error("Signup error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("errorCode", "SERVER_ERROR", "message", "Lỗi hệ thống"));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationDto dto) {
        logger.info("Verifying OTP for: {}", dto.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Người dùng không tồn tại!"
            ));
        }

        User user = userOpt.get();

        if (user.getCodeOTP() == null || !user.getCodeOTP().equals(dto.getOtp())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_OTP",
                    "message", "Mã OTP không hợp lệ!"
            ));
        }

        if (user.getOtpExpiryTime() != null && LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "OTP_EXPIRED",
                    "message", "Mã OTP đã hết hạn!"
            ));
        }

        user.setVerified(true);
        user.setCodeOTP(null);
        user.setOtpExpiryTime(null);
        userRepository.save(user);

        logger.info("User verified: {}", user.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "Tài khoản đã được xác thực! Bạn có thể đăng nhập."
        ));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        logger.info("Resend OTP for: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Người dùng không tồn tại!"
            ));
        }

        User user = userOpt.get();
        String otp = generateOtp();
        user.setCodeOTP(otp);
        user.setOtpExpiryTime(LocalDateTime.now().plusSeconds(60));
        userRepository.save(user);

        emailService.sendOtp(user.getEmail(), otp);
        return ResponseEntity.ok(Map.of(
                "message", "OTP đã được gửi lại"
        ));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginDto loginDto) {
        logger.info("Login attempt: {}", loginDto.getUsernameOrEmail());

        try {
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(
                    loginDto.getUsernameOrEmail(),
                    loginDto.getUsernameOrEmail()
            );

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "errorCode", "USER_NOT_FOUND",
                        "message", "Tên đăng nhập hoặc email không tồn tại!"
                ));
            }

            User user = userOpt.get();

            if (!Boolean.TRUE.equals(user.getVerified())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "errorCode", "USER_NOT_VERIFIED",
                        "message", "Tài khoản chưa được xác thực!"
                ));
            }

            if (user.getStatus() == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "errorCode", "USER_INACTIVE",
                        "message", "Tài khoản đã bị vô hiệu hóa!"
                ));
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            loginDto.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            logger.info("User logged in: {}", user.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "Đăng nhập thành công!",
                    "username", user.getUsername(),
                    "email", user.getEmail()
            ));
        } catch (Exception e) {
            logger.error("Login error", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_CREDENTIALS",
                    "message", "Tên đăng nhập hoặc mật khẩu không chính xác!"
            ));
        }
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendForgotPasswordOtp(@RequestBody ForgotPasswordRequestDto dto) {
        logger.info("Forgot password request: {}", dto.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Email không tồn tại!"
            ));
        }

        User user = userOpt.get();
        String otp = generateOtp();
        user.setCodeOTP(otp);
        user.setOtpExpiryTime(LocalDateTime.now().plusSeconds(60));
        user.setOtpPurpose("RESET_PASSWORD");
        userRepository.save(user);

        emailService.sendPasswordResetOtp(user.getEmail(), otp);
        return ResponseEntity.ok(Map.of(
                "message", "OTP đã được gửi tới email"
        ));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(@RequestBody ForgotPasswordOtpDto dto) {
        logger.info("Verify forgot password OTP: {}", dto.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Người dùng không tồn tại!"
            ));
        }

        User user = userOpt.get();

        if (user.getCodeOTP() == null || !user.getCodeOTP().equals(dto.getOtp())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_OTP",
                    "message", "Mã OTP không hợp lệ!"
            ));
        }

        if (user.getOtpExpiryTime() != null && LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "OTP_EXPIRED",
                    "message", "Mã OTP đã hết hạn!"
            ));
        }

        user.setCodeOTP(null);
        user.setOtpExpiryTime(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Xác thực thành công! Đặt mật khẩu mới."
        ));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto dto) {
        logger.info("Reset password: {}", dto.getEmail());

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "PASSWORD_MISMATCH",
                    "message", "Mật khẩu xác nhận không khớp!"
            ));
        }

        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Người dùng không tồn tại!"
            ));
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        logger.info("Password reset for: {}", user.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "Mật khẩu đã được đặt lại!"
        ));
    }
}