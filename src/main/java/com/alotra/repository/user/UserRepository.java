package com.alotra.repository.user;

import com.alotra.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	Optional<User> findByUsernameOrEmail(String username, String email);

	Optional<User> findById(Integer id);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByPhoneNumber(String phoneNumber);

	List<User> findByRoles_RoleName(String roleName);

	Optional<User> findByPhoneNumber(String phoneNumber);

	Optional<User> findByOtpCode(String otpCode);

}