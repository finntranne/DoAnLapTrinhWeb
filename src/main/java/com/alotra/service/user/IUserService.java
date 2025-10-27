package com.alotra.service.user;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.alotra.entity.user.User;


public interface IUserService {

	// ===== CRUD =====
    Optional<User> findById(Integer id);
    User save(User user);
    void deleteById(Integer id);
    boolean existsById(Integer id);
    long count();
    Page<User> findAll(Pageable pageable);

    // ===== SEARCH =====
    List<User> searchUsers(String username, String email, Integer roleid, Integer status, LocalDate startDate, LocalDate endDate, int page);
    int getTotalPages(String username, String email, Integer roleid, Integer status, LocalDate startDate, LocalDate endDate);

    // ===== EXTRA =====
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User updateUser(Integer id, User user);
	
    
}
