package com.alotra.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.alotra.entity.user.User;
import com.alotra.repository.user.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service("userDetailsService")
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user by username or email: {}", usernameOrEmail);
        
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    log.error("User not found: {}", usernameOrEmail);
                    return new UsernameNotFoundException("Could not find user with username or email: " + usernameOrEmail);
                });
        
        Integer shopId = null;
        
        try {
            // Kiểm tra user có shop không
            if (user.getShop() != null) {
                shopId = user.getShop().getShopId();
                log.debug("User {} has shop with ID: {}", usernameOrEmail, shopId);
            } else {
                log.debug("User {} does not have a shop", usernameOrEmail);
            }
        } catch (Exception e) {
            // Nếu lazy loading fail, shopId sẽ là null
            log.warn("Could not load shop for user {}: {}", usernameOrEmail, e.getMessage());
        }
        
        return new MyUserDetails(user, shopId);
    }
}