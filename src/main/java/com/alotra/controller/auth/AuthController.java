package com.alotra.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.naming.AuthenticationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.model.ForgotPasswordOtpDto;
import com.alotra.model.ForgotPasswordRequestDto;
import com.alotra.model.LoginDto;
import com.alotra.model.OtpVerificationDto;
import com.alotra.model.ResetPasswordDto;
import com.alotra.model.SignUpDto;
import com.alotra.repository.user.RoleRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.user.EmailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;



@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    
    // Inject SecurityContextRepository để quản lý session
    private SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginDto loginDto, HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginDto.getUsernameOrEmail(), loginDto.getPassword())
            );

            // 1. Lấy hoặc tạo một SecurityContext mới
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            
            // 2. Đặt thông tin xác thực vào context
            context.setAuthentication(authentication);
            
            // 3. Cập nhật SecurityContextHolder
            SecurityContextHolder.setContext(context);
            
            // 4. (QUAN TRỌNG NHẤT) Lưu context vào HttpSession để các request sau có thể sử dụng
            securityContextRepository.saveContext(context, request, response);

            return ResponseEntity.ok(Map.of("message", "Đăng nhập thành công!"));

        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Tên đăng nhập hoặc mật khẩu không đúng."));
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignUpDto signUpDto) {

    	if (userRepository.existsByUsername(signUpDto.getUsername())) {
            Map<String, String> error = new HashMap<>();
            error.put("errorCode", "USERNAME_TAKEN");
            error.put("message", "Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(signUpDto.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("errorCode", "EMAIL_TAKEN");
            error.put("message", "Email đã được sử dụng. Vui lòng nhập email khác.");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        // create user object
        User user = new User();
        user.setUsername(signUpDto.getUsername());
        user.setFullname(signUpDto.getFullname());
        user.setEmail(signUpDto.getEmail());
        user.setVerified(false);
        user.setPassword(passwordEncoder.encode(signUpDto.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setCodeOTP(otp);

        Role roles = roleRepository.findByRoleName("USER").get();
        Role detachedRole = new Role();
        detachedRole.setRoleId(roles.getRoleId());
        user.setRoles(Collections.singleton(roles));
        
        

        userRepository.save(user);
        
        emailService.sendOtp(user.getEmail(), otp);

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent to your email");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationDto dto) {

    	Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("errorCode", "USER_NOT_FOUND");
            error.put("message", "Người dùng không tồn tại!");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        User user = userOpt.get();

        if (user.getCodeOTP() == null || !user.getCodeOTP().equals(dto.getOtp())) {
            Map<String, String> error = new HashMap<>();
            error.put("errorCode", "INVALID_OTP");
            error.put("message", "Mã OTP không hợp lệ hoặc đã hết hạn!");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        // Xác thực thành công
        user.setVerified(true);
        user.setCodeOTP(null); 
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Tài khoản đã được xác thực thành công!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("errorCode", "USER_NOT_FOUND");
            error.put("message", "Người dùng không tồn tại!");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        User user = userOpt.get();

        // Tạo OTP mới
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setCodeOTP(otp);
        userRepository.save(user);

        // Gửi email OTP
        emailService.sendOtp(user.getEmail(), otp);

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP đã được gửi lại tới email của bạn");
        
        new Thread(() -> emailService.sendOtp(user.getEmail(), otp)).start();
        
        return ResponseEntity.ok(response);
    }
    
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("errorCode", "USER_NOT_FOUND");
            error.put("message", "Email không tồn tại trong hệ thống!");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        User user = userOpt.get();


        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setCodeOTP(otp);
        userRepository.save(user);

       
        emailService.sendOtp(user.getEmail(), otp);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Email đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư.");
        return ResponseEntity.ok(response);
    }


    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody ForgotPasswordRequestDto dto) {
        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Email không tồn tại trong hệ thống!"
            ), HttpStatus.BAD_REQUEST);
        }

        User user = userOpt.get();

        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setCodeOTP(otp);
        userRepository.save(user);
        System.out.println("OTP cho " + user.getEmail() + " là " + otp);

        emailService.sendOtp(user.getEmail(), otp);
        return ResponseEntity.ok(Map.of(
                "message", "OTP đã được gửi tới email của bạn."
        ));
    }


    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody ForgotPasswordOtpDto dto) {
        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Người dùng không tồn tại!"
            ), HttpStatus.BAD_REQUEST);
        }

        User user = userOpt.get();

        if (user.getCodeOTP() == null || !user.getCodeOTP().equals(dto.getOtp())) {
            return new ResponseEntity<>(Map.of(
                    "errorCode", "INVALID_OTP",
                    "message", "Mã OTP không hợp lệ hoặc đã hết hạn!"
            ), HttpStatus.BAD_REQUEST);
        }

        // OTP đúng → cho phép reset password
        user.setCodeOTP(null); // xóa OTP
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Xác thực OTP thành công!"
        ));
    }

  
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto dto) {
        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "Người dùng không tồn tại!"
            ), HttpStatus.BAD_REQUEST);
        }

        User user = userOpt.get();

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Mật khẩu đã được đặt lại thành công!"
        ));
    }
}

