//package com.alotra.entity.promotion;
//
//import com.alotra.entity.product.Product;
//<<<<<<< HEAD
//import jakarta.persistence.*;
//import lombok.*;
//=======
//
//import jakarta.persistence.EmbeddedId;
//import jakarta.persistence.Entity;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.ToString;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.MapsId;
//>>>>>>> lam
//
//@Entity
//@Table(name = "PromotionProducts")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//<<<<<<< HEAD
//public class PromotionProduct {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id; // Thêm một ID tự tăng cho bảng nối
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "PromotionID")
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Promotion promotion;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "ProductID")
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Product product;
//
//    @Column(name = "DiscountPercentage", nullable = false)
//    private Integer discountPercentage;
//}
//=======
//@ToString(exclude = { "promotion", "product" })
//public class PromotionProduct {
//
//	@EmbeddedId
//	private PromotionProductId id;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@MapsId("promotionID")
//	@JoinColumn(name = "PromotionID")
//	private Promotion promotion;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@MapsId("productID")
//	@JoinColumn(name = "ProductID")
//	private Product product;
//}
//
//>>>>>>> lam


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
@ToString(exclude = { "promotion", "product" }) // Giữ Exclude từ nhánh lam
@EqualsAndHashCode(exclude = { "promotion", "product" }) // Thêm EqualsAndHashCode Exclude
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

//// Lớp Id phức hợp (Composite Key Class) - Giữ lại từ nhánh lam
//@Embeddable
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@EqualsAndHashCode // Id class cần equals/hashCode
//class PromotionProductId implements Serializable { // Implement Serializable
//
//	@Column(name = "PromotionID") // Thêm @Column để khớp DB
//	private Integer promotionID; // Tên khớp @MapsId
//
//	@Column(name = "ProductID") // Thêm @Column để khớp DB
//	private Integer productID; // Tên khớp @MapsId
//}