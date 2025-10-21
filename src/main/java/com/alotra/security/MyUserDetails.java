package com.alotra.security;

import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;

public class MyUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;
    private final User user;
    private final Integer shopId; // 🔹 thêm thuộc tính shopId

    // Constructor mới nhận thêm shopId
    public MyUserDetails(User user, Integer shopId) {
        this.user = user;
        this.shopId = shopId;
    }

    // Constructor cũ (nếu không có shopId)
    public MyUserDetails(User user) {
        this(user, null);
    }

    // ✅ Getter cho shopId
    public Integer getShopId() {
        return shopId;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // Dùng email làm username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.user.getStatus() == 1;
    }
}
