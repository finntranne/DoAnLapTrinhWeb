package com.alotra.service.user;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.alotra.entity.user.User;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    	User byLogin = userService.findByLogin(username);
        if (byLogin == null) {
            throw new UsernameNotFoundException("Không tìm thấy tài khoản: " + username);
        }
        
        boolean enabled = byLogin.getStatus() == 1 && Boolean.TRUE.equals(byLogin.getIsVerified());

        return org.springframework.security.core.userdetails.User.builder()
                .username(byLogin.getUsername())
                .password(byLogin.getPassword())
                .roles(byLogin.getRole().getRolename()) 
                .disabled(!enabled)
                .build();
    }
}
