package com.alotra.entity.order;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

//Lớp Id phức hợp (Composite Key Class)
@Embeddable // Đánh dấu là Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // Id class nên implement equals và hashCode
public class OrderDetailToppingId implements Serializable { // Implement Serializable

	private static final long serialVersionUID = 1L;

	// Tên thuộc tính phải khớp với giá trị trong @MapsId ở trên
	@Column(name = "OrderDetailID") // Thêm @Column để chắc chắn khớp
	private Integer orderDetailID;

	@Column(name = "ToppingID") // Thêm @Column để chắc chắn khớp
	private Integer toppingID;
}