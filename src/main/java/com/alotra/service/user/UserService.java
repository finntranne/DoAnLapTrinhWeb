package com.alotra.service.user;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.alotra.config.jwt.JwtProvider;
import com.alotra.controller.request.UserTokenRequest;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.execption.AuthException;
import com.alotra.repository.user.RoleRepository;
import com.alotra.repository.user.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Autowired
    RoleRepository roleRepository;
    
    @Autowired
    UserRepository userRepository;

    public void register(User user, String otp) {
    	Role role = roleRepository.findByRolename("USER");
        if (role == null) {
            throw new RuntimeException("Role 'USER' không tồn tại trong database!");
        }

        user.setRole(role);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setIsVerified(false);
        user.setStatus(1);
        user.setCodeOTP(otp);
        user.setOtpCreatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    public User findByLogin(String login) {
    	Optional<User> user = userRepository.findByUsername(login);

        if (user.isEmpty()) {
            user = userRepository.findByEmail(login);
        }
        return user.orElse(null);
    }

    public String generateToken(UserTokenRequest request) {
        var userModel = Optional.ofNullable(findByLogin(request.getLogin()))
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .orElseThrow(AuthException::new);

        return jwtProvider.generateToken(userModel.getUsername());
    }

    public String generateOTP() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }
    
    public User findByEmail(String email) {
    	Optional<User> user = userRepository.findByEmail(email); 	
    	return user.orElse(null);
    }
    
    public void verifyOtp(User user) {
    	user.setIsVerified(true);
        user.setCodeOTP(null);
        user.setOtpCreatedAt(null);
        userRepository.save(user);
    }
}
