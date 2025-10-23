package com.alotra.service.user;

import com.alotra.entity.user.User;
import java.util.Optional;

public interface UserService {
    // Thêm hàm này (nếu chưa có)
    Optional<User> findByUsername(String username);

    // (Thêm các hàm quản lý User khác nếu cần: save, findById, etc.)
    
    public User save(User user);
}