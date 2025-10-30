package com.alotra.security;

import java.time.LocalDateTime;
import java.util.HashSet;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.alotra.entity.user.User;
import com.alotra.repository.user.UserRepository;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        if (email == null) {
            throw new OAuth2AuthenticationException("Google account does not have an email!");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setUsername(email); // bạn có thể tách username khác nếu muốn
            newUser.setPassword("GOOGLE_LOGIN"); // không dùng thật, chỉ placeholder
            newUser.setAvatarURL(picture);
            newUser.setStatus((byte) 1); // Active
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            newUser.setRoles(new HashSet<>()); // có thể set ROLE_USER mặc định

            return userRepository.save(newUser);
        });

        // cập nhật thông tin nếu người dùng đã có
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return oAuth2User;
    }
}