package com.alotra.repository.user;

import com.alotra.entity.user.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByOtpCode(String codeOTP);
    Optional<User> findById(Integer id);
    
    @Query("""
    	    SELECT DISTINCT u FROM User u
    	    JOIN u.roles r
    	    WHERE r.roleName <> 'ADMIN'
    	""")
    	Page<User> findAllWithoutAdmin(Pageable pageable);
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    
    List<User> findByRoles_RoleName(String roleName);
    
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    void deleteById(Integer id);
    boolean existsById(Integer id);
    
    
    
    @Query("""
    	    SELECT DISTINCT u FROM User u
    	    LEFT JOIN u.roles r
    	    WHERE (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
    	      AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
    	      AND (:phoneNumber IS NULL OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :phoneNumber, '%')))
    	      AND (:roleId IS NULL OR r.id = :roleId)
    	      AND (:status IS NULL OR u.status = :status)
    	      AND (:startDate IS NULL OR u.createdAt >= :startDate)
    	      AND (:endDate IS NULL OR u.createdAt <= :endDate)
    	""")
    	Page<User> searchUsers(
    	        @Param("username") String username,
    	        @Param("email") String email,
    	        @Param("phoneNumber") String phoneNumber,
    	        @Param("roleId") Integer roleId,
    	        @Param("status") Integer status,
    	        @Param("startDate") LocalDateTime startDate,
    	        @Param("endDate") LocalDateTime endDate,
    	        Pageable pageable
    	);

}