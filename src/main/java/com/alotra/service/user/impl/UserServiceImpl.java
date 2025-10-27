package com.alotra.service.user.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority; // Thêm import
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Thêm import
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // Thêm import
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.alotra.entity.user.User;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.user.UserService;

import java.util.Collection; // Thêm import
import java.util.Optional;
import java.util.stream.Collectors; // Thêm import

@Service
// Sửa đổi: implements cả hai interface
public class UserServiceImpl implements UserService, UserDetailsService { // <<< SỬA Ở ĐÂY

    @Autowired
    private UserRepository userRepository;

    // --- Hàm của UserDetailsService (Bỏ comment và Sửa) ---
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) // Tham số là username hoặc email
            throws UsernameNotFoundException {

        // Tìm user bằng username HOẶC email
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail) // <<< SỬA Ở ĐÂY
                .orElseThrow(() ->
                        new UsernameNotFoundException("Không tìm thấy người dùng với username hoặc email: " + usernameOrEmail));

        // Lấy danh sách quyền từ Set<Role> của User entity
        Collection<? extends GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName())) // Giả sử Role entity có getRoleName()
                .collect(Collectors.toList());

        // Trả về đối tượng UserDetails chuẩn của Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // <<< QUAN TRỌNG: Dùng email làm username cho UserDetails (hoặc username tùy bạn chọn)
                user.getPassword(), // Password đã hash
                authorities // Danh sách quyền
                // Các tham số khác (enabled, accountNonExpired, etc.) có thể thêm nếu User entity có
        );
    }

    // === CÁC HÀM CỦA UserService (Giữ nguyên) ===
    @Override
    public Optional<User> findByUsername(String username) {
        // Có thể cần sửa lại nếu userRepository không có hàm này,
        // hoặc dùng lại findByUsernameOrEmail
        // return userRepository.findByUsername(username);
         return userRepository.findByUsernameOrEmail(username, username); // Tạm dùng lại hàm này
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    // --- THÊM HÀM tìm theo Email (quan trọng cho loadUserByUsername) ---
    @Override
    public Optional<User> findByEmail(String email) {
         return userRepository.findByUsernameOrEmail(email, email); // Tạm dùng lại hàm này
        // Hoặc nếu repo có: return userRepository.findByEmail(email);
    }
}