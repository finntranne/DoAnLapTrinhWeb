
package com.alotra.entity.promotion; // Giữ package này

import com.alotra.entity.product.Product;

import jakarta.persistence.*; // Import đầy đủ JPA annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude
import java.io.Serializable; // Import Serializable

@Entity
@Table(name = "PromotionProducts") // Khớp DB
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "promotion", "product" }) 
@EqualsAndHashCode(exclude = { "promotion", "product" }) 
public class PromotionProduct {

	@EmbeddedId // Sử dụng khóa chính phức hợp từ nhánh lam
	private PromotionProductId id;

	@ManyToOne(fetch = FetchType.LAZY) // Giữ LAZY fetch
	@MapsId("promotionID") // Ánh xạ tới thuộc tính trong Id class, khớp Id class
	@JoinColumn(name = "PromotionID") // Khớp DB
	private Promotion promotion;

	@ManyToOne(fetch = FetchType.LAZY) // Giữ LAZY fetch
	@MapsId("productID") // Ánh xạ tới thuộc tính trong Id class, khớp Id class
	@JoinColumn(name = "ProductID") // Khớp DB
	private Product product;

	@Column(name = "DiscountPercentage", nullable = false)
	private Integer discountPercentage = 0;
	// Bỏ các trường id (surrogate key) và discountPercentage từ HEAD
}
