package com.alotra.service.user;

import com.alotra.entity.user.User;
import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email); // <<< THÊM HÀM NÀY
    User save(User user);
    Optional<User> findByPhoneNumber(String phoneNumber);
}