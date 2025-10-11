package com.alotra.entity.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

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

    @Column(name = "FullName", length = 255)
    private String fullname;

    @Column(name = "Status", nullable = false)
    private Byte status = 1; // 0=Inactive, 1=Active (TINYINT)

    @Column(name = "AvatarURL", length = 500)
    private String avatarURL;

    @Column(name = "CreatedAt", nullable = false, columnDefinition = "DATETIME2")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false, columnDefinition = "DATETIME2")
    private LocalDateTime updatedAt;

    @Column(name = "OtpCode", length = 10)
    private String codeOTP;

    @Column(name = "OtpExpiryTime", columnDefinition = "DATETIME2")
    private LocalDateTime otpExpiryTime;

    @Column(name = "OtpPurpose", length = 20)
    private String otpPurpose; // REGISTER, RESET_PASSWORD

    @Column(name = "Verified", columnDefinition = "BIT DEFAULT 0")
    private Boolean verified = false;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
        name = "UserRoles",
        joinColumns = @JoinColumn(name = "UserID", referencedColumnName = "UserID"),
        inverseJoinColumns = @JoinColumn(name = "RoleID", referencedColumnName = "RoleID")
    )
    private Set<Role> roles;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = 1;
        }
        if (verified == null) {
            verified = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}