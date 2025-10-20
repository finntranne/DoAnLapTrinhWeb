package com.alotra.entity.order;

import java.math.BigDecimal;

import com.alotra.entity.product.Topping;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "OrderDetail_Toppings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "orderDetail", "topping" })
public class OrderDetailTopping {

	@EmbeddedId
	private OrderDetailToppingId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("orderDetailID")
	@JoinColumn(name = "OrderDetailID")
	private OrderDetail orderDetail;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("toppingID")
	@JoinColumn(name = "ToppingID")
	private Topping topping;

	@Column(name = "UnitPrice", nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;
}

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class OrderDetailToppingId implements java.io.Serializable {
	private Integer orderDetailID;
	private Integer toppingID;
}
