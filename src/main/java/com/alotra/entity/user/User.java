package com.alotra.entity.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

import com.alotra.entity.shop.Shop;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "UserID")
	private Integer id;

	@Column(name = "Username", nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "PasswordHash", nullable = false, length = 255)
	private String password;

	@Column(name = "Email", nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "PhoneNumber", unique = true, length = 20)
	private String phoneNumber;

	@Column(name = "FullName", nullable = false, length = 255)
	private String fullName;

	@Column(name = "Status", nullable = false)
	private Byte status = 0; // 0: Pending, 1: Active, 2: Suspended

	@Column(name = "AvatarURL", length = 500)
	private String avatarURL;

	// OTP fields
	@Column(name = "OtpCode", columnDefinition = "VARCHAR(MAX)")
	private String otpCode;

	@Column(name = "OtpExpiryTime", columnDefinition = "DATETIME2")
	private LocalDateTime otpExpiryTime;

	@Column(name = "OtpPurpose", length = 20)
	private String otpPurpose;

	@Column(name = "CreatedAt", nullable = false, columnDefinition = "DATETIME2")
	private LocalDateTime createdAt;

	@Column(name = "UpdatedAt", nullable = false, columnDefinition = "DATETIME2")
	private LocalDateTime updatedAt;

	@Column(name = "LastLoginAt", columnDefinition = "DATETIME2")
	private LocalDateTime lastLoginAt;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
	@JoinTable(name = "UserRoles", joinColumns = @JoinColumn(name = "UserID", referencedColumnName = "UserID"), inverseJoinColumns = @JoinColumn(name = "RoleID", referencedColumnName = "RoleID"))
	private Set<Role> roles;

	@OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
	private Shop shop;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
		if (status == null) {
			status = 0; // Default: Pending
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}