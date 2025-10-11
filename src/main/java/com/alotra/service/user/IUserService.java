package com.alotra.service.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.alotra.entity.user.User;


public interface IUserService {

	// ===== CRUD =====
    List<User> findAll();
    Optional<User> findById(Integer id);
    User save(User user);
    void deleteById(Integer id);
    boolean existsById(Integer id);
    long count();

    // ===== SEARCH =====
    List<User> findByUsernameContaining(String username);
    Page<User> findByUsernameContaining(String username, Pageable pageable);

    // ===== EXTRA =====
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User updateUser(Integer id, User user);
}
