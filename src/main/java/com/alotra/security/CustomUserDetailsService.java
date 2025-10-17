package com.alotra.security;

import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        logger.info("=== Loading user: {} ===", usernameOrEmail);

        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", usernameOrEmail);
                    return new UsernameNotFoundException("User not found: " + usernameOrEmail);
                });

        Set<Role> roles = user.getRoles();
        logger.info("Raw roles from DB: {}", roles);
        logger.info("Number of roles: {}", roles != null ? roles.size() : 0);
        
        if (roles != null) {
            roles.forEach(role -> logger.info("Role: id={}, name={}", role.getId(), role.getRoleName()));
        }

        Collection<? extends GrantedAuthority> authorities = mapRolesToAuthorities(roles);
        logger.info("Final authorities: {}", authorities);
        authorities.forEach(auth -> logger.info("Authority: {}", auth.getAuthority()));

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(user.getStatus() != null && user.getStatus() != 1)
                .build();

        logger.info("UserDetails created. Enabled: {}", userDetails.isEnabled());
        return userDetails;
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            logger.warn("No roles found for user");
            return Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        Set<GrantedAuthority> authorities = roles.stream()
                .map(role -> {
                    String roleName = role.getRoleName();
                    // Đảm bảo role name không có space và in hoa
                    String authority = "ROLE_" + roleName.toUpperCase().trim();
                    logger.info("Mapping: {} -> {}", roleName, authority);
                    return new SimpleGrantedAuthority(authority);
                })
                .collect(Collectors.toSet());

        logger.info("Total authorities mapped: {}", authorities.size());
        return authorities;
    }
}