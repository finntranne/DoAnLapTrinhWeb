package com.alotra.controller;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.alotra.entity.user.User;
import com.alotra.model.user.UserModel;
import com.alotra.service.user.EmailService;
import com.alotra.service.user.UserService;

@Controller
@RequiredArgsConstructor
public class UserLoginController {

    private final UserService userService;
    
    @Autowired
    EmailService emailService;

    @GetMapping("/login")
    public String getLoginPage() {
        return "login";
    }

    @GetMapping("/registration")
    public String getRegistrationPage(Model model) {
        model.addAttribute("user", new UserModel());
        return "registration_page";
    }

    @PostMapping("/registration")
    public String registerUser(@ModelAttribute User user, Model model) {
    	String otp = userService.generateOTP();
        userService.register(user, otp);
        emailService.sendOtp(user.getEmail(), otp);
        model.addAttribute("user", new UserModel());
        return "verifyOTP";
    }
    
    @PostMapping("/verifyOTP")
    public String verifyOtp(@ModelAttribute User user) {
    	User existingUser = userService.findByEmail(user.getEmail());
        if (existingUser == null) return "verifyFail";
        if (!user.getCodeOTP().equals(existingUser.getCodeOTP())) return "verifyFail";

        LocalDateTime now = LocalDateTime.now();
        if (existingUser.getOtpCreatedAt() == null ||
            existingUser.getOtpCreatedAt().plusSeconds(60).isBefore(now)) {
            return "otpExpired";
        }

        userService.verifyOtp(existingUser);
        return "redirect:/login";
    }


 
}
