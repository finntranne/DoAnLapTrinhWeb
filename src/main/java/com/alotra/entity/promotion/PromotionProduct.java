package com.alotra.entity.promotion;

import com.alotra.entity.product.Product;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

@Entity
@Table(name = "PromotionProducts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "promotion", "product" })
public class PromotionProduct {

	@EmbeddedId
	private PromotionProductId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("promotionID")
	@JoinColumn(name = "PromotionID")
	private Promotion promotion;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("productID")
	@JoinColumn(name = "ProductID")
	private Product product;
}

