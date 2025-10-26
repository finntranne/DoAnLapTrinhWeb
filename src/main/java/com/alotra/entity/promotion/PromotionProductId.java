package com.alotra.entity.promotion;

import groovy.transform.EqualsAndHashCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // Id class cần equals/hashCode
public class PromotionProductId implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Column(name = "PromotionID") // Thêm @Column để khớp DB
	private Integer promotionID; // Tên khớp @MapsId

	@Column(name = "ProductID") // Thêm @Column để khớp DB
	private Integer productID; // Tên khớp @MapsId
}