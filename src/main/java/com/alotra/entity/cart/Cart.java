package com.alotra.entity.cart;

import java.time.LocalDateTime;

import com.alotra.entity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CartID")
	private Integer cartID;

	@OneToOne
	@JoinColumn(name = "UserID", nullable = false, unique = true)
	private User user;

	@Column(name = "CreatedAt", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "UpdatedAt", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
}
