package com.alotra.entity.shop;


import java.time.LocalDateTime;

import com.alotra.entity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "Shops")
public class Shop {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShopID")
	private int shopId;
	
	@Column(name = "ShopName", columnDefinition = "NVARCHAR(255)")
	private String shopName;
	
	@OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false, unique = true)
    private User owner;
	
	@Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
	private String description;
	
	@Column(name = "LogoURL")
	private String logoUrl;
	
	@Column(name = "CoverImageURL")
	private String coverImageUrl;
	
	@Column(name = "Address", columnDefinition = "NVARCHAR(MAX)")
	private String address;
	
	@Column(name = "PhoneNumber")
	private String phone;

	@Column(name = "CreatedAt")
	private LocalDateTime createdAt;
}
