package com.alotra.repository.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.user.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

	@Query("SELECT u FROM User u WHERE u.email = :input OR u.username = :input")
    public User getUserByUsernameOrEmail(@Param("input") String input);
    
    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
    
    List<User> findByUsernameContaining(String username);
    Page<User> findByUsernameContaining(String username, Pageable pageable);
    
    @Query("SELECT u FROM User u " +
    	       "WHERE (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) " +
    	       "AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
    	       "AND (:roleId IS NULL OR u.role.id = :roleId) " +
    	       "AND (:status IS NULL OR u.status = :status) " +
    	       "AND (:startDate IS NULL OR u.createdAt >= :startDate) " +
    	       "AND (:endDate IS NULL OR u.createdAt <= :endDate)")
    	Page<User> searchUsers(
    	        @Param("username") String username,
    	        @Param("email") String email,
    	        @Param("roleId") Integer roleId,
    	        @Param("status") Integer status,
    	        @Param("startDate") LocalDateTime startDate,
    	        @Param("endDate") LocalDateTime endDate,
    	        Pageable pageable);

	

}
