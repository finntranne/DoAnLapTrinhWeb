//package com.alotra.entity.order;
//
//<<<<<<< HEAD
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//=======
//>>>>>>> lam
//import java.math.BigDecimal;
//
//import com.alotra.entity.product.Topping;
//
//<<<<<<< HEAD
//=======
//import jakarta.persistence.Column;
//import jakarta.persistence.Embeddable;
//import jakarta.persistence.EmbeddedId;
//import jakarta.persistence.Entity;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.MapsId;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.ToString;
//
//>>>>>>> lam
//@Entity
//@Table(name = "OrderDetail_Toppings")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//<<<<<<< HEAD
//public class OrderDetailTopping {
//
//    @EmbeddedId
//    private OrderDetailToppingId id;
//
//    @ManyToOne
//    @MapsId("orderDetailId")
//    @JoinColumn(name = "OrderDetailID")
//    private OrderDetail orderDetail;
//
//    @ManyToOne
//    @MapsId("toppingId")
//    @JoinColumn(name = "ToppingID")
//    private Topping topping;
//
//    @Column(name = "Quantity", nullable = false)
//    private Integer quantity;
//
//    @Column(name = "UnitPrice", nullable = false)
//    private BigDecimal unitPrice;
//
//    @Column(name = "LineTotal", nullable = false)
//    private BigDecimal lineTotal;
//=======
//@ToString(exclude = { "orderDetail", "topping" })
//public class OrderDetailTopping {
//
//	@EmbeddedId
//	private OrderDetailToppingId id;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@MapsId("orderDetailID")
//	@JoinColumn(name = "OrderDetailID")
//	private OrderDetail orderDetail;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@MapsId("toppingID")
//	@JoinColumn(name = "ToppingID")
//	private Topping topping;
//
//	@Column(name = "UnitPrice", nullable = false, precision = 10, scale = 2)
//	private BigDecimal unitPrice;
//>>>>>>> lam
//}
//
//@Embeddable
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//<<<<<<< HEAD
//class OrderDetailToppingId {
//    private Integer orderDetailId;
//    private Integer toppingId;
//=======
//class OrderDetailToppingId implements java.io.Serializable {
//	private Integer orderDetailID;
//	private Integer toppingID;
//>>>>>>> lam
//}


package com.alotra.entity.order; // Giữ package này

import java.math.BigDecimal;
import java.io.Serializable; // Import Serializable cho Id class

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
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

@Entity
@Table(name = "OrderDetail_Toppings") // Khớp DB
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "orderDetail", "topping" }) // Giữ Exclude từ nhánh lam
@EqualsAndHashCode(exclude = { "orderDetail", "topping" }) // Thêm EqualsAndHashCode Exclude
public class OrderDetailTopping {

	@EmbeddedId // Sử dụng khóa chính phức hợp
	private OrderDetailToppingId id;

	@ManyToOne(fetch = FetchType.LAZY) // Giữ LAZY fetch
	@MapsId("orderDetailID") // Ánh xạ tới thuộc tính trong Id class
	@JoinColumn(name = "OrderDetailID") // Khớp tên cột DB
	private OrderDetail orderDetail;

	@ManyToOne(fetch = FetchType.LAZY) // Giữ LAZY fetch
	@MapsId("toppingID") // Ánh xạ tới thuộc tính trong Id class
	@JoinColumn(name = "ToppingID") // Khớp tên cột DB
	private Topping topping;

	// Giá của topping tại thời điểm mua
	@Column(name = "UnitPrice", nullable = false, precision = 10, scale = 2) // Khớp DB
	private BigDecimal unitPrice;

	// Bỏ các trường Quantity, LineTotal từ HEAD vì không có trong DB nhánh lam
}

