package com.alotra.dto.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserModel implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private Integer id;
	private String username;
	private String email;
	private String password;
	private String phoneNumber;
	private String fullName;
	private String avatarURL;
	private Byte status;
	private LocalDateTime createdAt;
	private LocalDateTime updateddAt;
	private LocalDateTime lastLoginAt;
	private String otpCode;
	private LocalDateTime otpExpiryTime;
	private String otpPurpose;
	
	private Set<Role> roles;
	private Shop shop;
	
	private MultipartFile file;
}