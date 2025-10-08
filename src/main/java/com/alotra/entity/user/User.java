package com.alotra.entity.user;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "Users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "UserID")
	private int user_id;
	
	@Column(name = "UserName",  unique = true)
	private String username;
	
	@Column(name = "Email",  unique = true)
	private String email;
	
	@Column(name = "Password" )
	private String password;
	
	@Column(name = "FullName", columnDefinition = "nvarchar(200)", unique = true)
	private String fullname;
	
	@Column(name = "Avatar")
	private String avatar;
	
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
        name = "Users_Roles",
        joinColumns = @JoinColumn(name = "UserID"),
        inverseJoinColumns = @JoinColumn(name = "RoleID")
    )
    private Set<Role> roles = new HashSet<>();
	
	@Column(name = "Status")
	private int status;
	

	@Column(name = "CreatedAt")
	private LocalDateTime createdAt;
	
	@Column(name = "IsVerified")
	private boolean isVerified;
}
