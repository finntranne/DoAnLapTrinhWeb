package com.alotra.model.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import com.alotra.entity.user.Role;

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
	private int user_id;
	private String username;
	private String email;
	private String password;
	private String fullname;
	private String avatar;
	private Integer status;
	private LocalDateTime createdAt;
	private Boolean isVerified;
	private String codeOTP;
	
	private Role role;
	
	private MultipartFile file;
}
