package com.alotra.entity.cart;

import java.time.LocalDateTime;

import com.alotra.entity.product.ProductVariant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CartItems")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CartItemID")
	private Integer cartItemID;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CartID", nullable = false)
	private Cart cart;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VariantID", nullable = false)
	private ProductVariant variant;

	@Column(name = "Quantity", nullable = false)
	private Integer quantity;

	@Column(name = "AddedAt", nullable = false)
	private LocalDateTime addedAt = LocalDateTime.now();
}
