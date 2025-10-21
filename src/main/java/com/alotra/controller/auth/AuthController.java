package com.alotra.controller.auth;

import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.dto.auth.*;
import com.alotra.repository.user.RoleRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.user.EmailService;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
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
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpDto signUpDto, BindingResult result) {
        logger.info("Signup request for username: {}", signUpDto.getUsername());

        // Validate input
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "VALIDATION_ERROR",
                    "message", "Dữ liệu không hợp lệ",
                    "errors", errors
                ));
        }

        // Check username exists
        if (userRepository.existsByUsername(signUpDto.getUsername())) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "USERNAME_TAKEN",
                    "message", "Tên đăng nhập đã tồn tại."
                ));
        }

        // Check email exists
        if (userRepository.existsByEmail(signUpDto.getEmail())) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "EMAIL_TAKEN",
                    "message", "Email đã được sử dụng."
                ));
        }

        // Check phone number if provided
        if (signUpDto.getPhoneNumber() != null && !signUpDto.getPhoneNumber().trim().isEmpty()) {
            String phoneNumber = signUpDto.getPhoneNumber().trim();
            if (userRepository.existsByPhoneNumber(phoneNumber)) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "errorCode", "PHONE_TAKEN",
                        "message", "Số điện thoại đã được sử dụng."
                    ));
            }

            // Normalize phone number
            if (phoneNumber.startsWith("0")) {
                phoneNumber = "+84" + phoneNumber.substring(1);
            }
            signUpDto.setPhoneNumber(phoneNumber);
        } else {
            signUpDto.setPhoneNumber(null);
        }

        try {
            User user = new User();
            user.setUsername(signUpDto.getUsername());
            user.setEmail(signUpDto.getEmail());
            user.setPhoneNumber(signUpDto.getPhoneNumber());
            user.setFullName(signUpDto.getFullname());
            user.setPassword(passwordEncoder.encode(signUpDto.getPassword()));
            user.setStatus((byte) 0); // 0 = Pending (chưa xác thực OTP)

            // Generate OTP
            String otp = generateOtp();
            user.setOtpCode(otp);
            user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
            user.setOtpPurpose("REGISTER");

            // Assign CUSTOMER role
            Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Customer role not found"));
            user.setRoles(new HashSet<>(Collections.singletonList(customerRole)));

            // Save user
            user = userRepository.save(user);

            // Send OTP email
            try {
                emailService.sendOtp(user.getEmail(), otp);
            } catch (Exception e) {
                logger.error("Failed to send OTP email", e);
            }

            logger.info("User registered: {}", signUpDto.getUsername());
            return ResponseEntity.ok(Map.of(
                "message", "OTP đã được gửi tới email",
                "email", user.getEmail()
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
                .body(Map.of(
                    "errorCode", "DATA_INTEGRITY_ERROR",
                    "message", errorMessage
                ));
        } catch (Exception e) {
            logger.error("Signup error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "errorCode", "SERVER_ERROR",
                    "message", "Lỗi hệ thống. Vui lòng thử lại sau."
                ));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerificationDto dto, BindingResult result) {
        logger.info("Verifying OTP for: {}", dto.getEmail());

        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "VALIDATION_ERROR",
                    "message", "Dữ liệu không hợp lệ",
                    "errors", errors
                ));
        }

        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Người dùng không tồn tại!"
                ));
        }

        User user = userOpt.get();

        if (user.getOtpCode() == null || !user.getOtpCode().equals(dto.getOtp())) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "INVALID_OTP",
                    "message", "Mã OTP không hợp lệ!"
                ));
        }

        if (user.getOtpExpiryTime() != null && LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "OTP_EXPIRED",
                    "message", "Mã OTP đã hết hạn!"
                ));
        }

        user.setStatus((byte) 1); // Active
        user.setOtpCode(null);
        user.setOtpExpiryTime(null);
        user.setOtpPurpose(null);
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
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Người dùng không tồn tại!"
                ));
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
                .body(Map.of(
                    "errorCode", "EMAIL_ERROR",
                    "message", "Không thể gửi OTP. Vui lòng thử lại sau."
                ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "OTP đã được gửi lại"
        ));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody LoginDto loginDto,
            BindingResult result,
            HttpServletRequest request) {
        
        logger.info("Login attempt: {}", loginDto.getUsernameOrEmail());

        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "VALIDATION_ERROR",
                    "message", "Dữ liệu không hợp lệ",
                    "errors", errors
                ));
        }

        try {
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(
                loginDto.getUsernameOrEmail(),
                loginDto.getUsernameOrEmail()
            );

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "errorCode", "USER_NOT_FOUND",
                        "message", "Tên đăng nhập hoặc email không tồn tại!"
                    ));
            }

            User user = userOpt.get();

            // Check account status
            if (user.getStatus() == 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "errorCode", "USER_NOT_VERIFIED",
                        "message", "Tài khoản chưa được xác thực!",
                        "email", user.getEmail()
                    ));
            }

            if (user.getStatus() == 2) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "errorCode", "USER_SUSPENDED",
                        "message", "Tài khoản đã bị tạm khóa!"
                    ));
            }

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    loginDto.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Create session
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // Update last login time
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            logger.info("User logged in: {}", user.getUsername());

            return ResponseEntity.ok(Map.of(
                "message", "Đăng nhập thành công!",
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "redirectUrl", "vendor/dashboard"
            ));

        } catch (Exception e) {
            logger.error("Login error", e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "INVALID_CREDENTIALS",
                    "message", "Tên đăng nhập hoặc mật khẩu không chính xác!"
                ));
        }
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordRequestDto dto,
            BindingResult result) {
        
        logger.info("Forgot password request: {}", dto.getEmail());

        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "VALIDATION_ERROR",
                    "message", "Dữ liệu không hợp lệ",
                    "errors", errors
                ));
        }

        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Email không tồn tại!"
                ));
        }

        User user = userOpt.get();
        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        user.setOtpPurpose("RESET_PASSWORD");
        userRepository.save(user);

        try {
            emailService.sendPasswordResetOtp(user.getEmail(), otp);
        } catch (Exception e) {
            logger.error("Failed to send password reset OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "errorCode", "EMAIL_ERROR",
                    "message", "Không thể gửi OTP. Vui lòng thử lại sau."
                ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "OTP đã được gửi tới email"
        ));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordOtpDto dto,
            BindingResult result) {
        
        logger.info("Verify forgot password OTP: {}", dto.getEmail());

        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "VALIDATION_ERROR",
                    "message", "Dữ liệu không hợp lệ",
                    "errors", errors
                ));
        }

        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Người dùng không tồn tại!"
                ));
        }

        User user = userOpt.get();

        if (user.getOtpCode() == null || !user.getOtpCode().equals(dto.getOtp())) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "INVALID_OTP",
                    "message", "Mã OTP không hợp lệ!"
                ));
        }

        if (user.getOtpExpiryTime() != null && LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "OTP_EXPIRED",
                    "message", "Mã OTP đã hết hạn!"
                ));
        }

        // Don't clear OTP yet - keep it for the reset password step
        return ResponseEntity.ok(Map.of(
            "message", "Xác thực thành công! Đặt mật khẩu mới."
        ));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordDto dto,
            BindingResult result) {
        
        logger.info("Reset password: {}", dto.getEmail());

        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "VALIDATION_ERROR",
                    "message", "Dữ liệu không hợp lệ",
                    "errors", errors
                ));
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "PASSWORD_MISMATCH",
                    "message", "Mật khẩu xác nhận không khớp!"
                ));
        }

        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Người dùng không tồn tại!"
                ));
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setOtpCode(null);
        user.setOtpExpiryTime(null);
        user.setOtpPurpose(null);
        userRepository.save(user);

        logger.info("Password reset for: {}", user.getEmail());
        return ResponseEntity.ok(Map.of(
            "message", "Mật khẩu đã được đặt lại thành công!"
        ));
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

            return ResponseEntity.ok(Map.of(
                "message", "Đăng xuất thành công!"
            ));
        } catch (Exception e) {
            logger.error("Logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "errorCode", "LOGOUT_ERROR",
                    "message", "Lỗi khi đăng xuất. Vui lòng thử lại."
                ));
        }
    }
}