package com.alotra.entity.user;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "Users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "Username", nullable = false, unique = true)
    private String username;

    @Column(name = "PasswordHash", nullable = false)
    private String passwordHash;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @Column(name = "PhoneNumber", unique = true)
    private String phoneNumber;

    @Column(name = "FullName")
    private String fullName;

    @Column(name = "Avatar")
    private String avatar;

    @Column(name = "IsVerified", nullable = false)
    private Boolean isVerified;

    @Column(name = "Status", nullable = false)
    private Integer status;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

	public Set<Role> getRoles() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setRoles(Set<Role> singleton) {
		// TODO Auto-generated method stub
		
	}
}
