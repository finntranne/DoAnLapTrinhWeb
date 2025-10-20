package com.alotra.entity.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.alotra.entity.location.ShippingProvider;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "Orders", indexes = { @Index(name = "IX_Orders_UserID", columnList = "UserID"),
		@Index(name = "IX_Orders_ShopID", columnList = "ShopID"),
		@Index(name = "IX_Orders_OrderStatus", columnList = "OrderStatus") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "user", "shop", "promotion", "shipper", "orderDetails" })
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "OrderID")
	private Integer orderID;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "UserID", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ShopID", nullable = false)
	private Shop shop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PromotionID")
	private Promotion promotion;

	@Column(name = "OrderDate", nullable = false)
	private LocalDateTime orderDate = LocalDateTime.now();

	@Column(name = "OrderStatus", nullable = false, length = 30)
	private String orderStatus = "Pending";

	@Column(name = "PaymentMethod", nullable = false, length = 50)
	private String paymentMethod;

	@Column(name = "PaymentStatus", nullable = false, length = 30)
	private String paymentStatus = "Unpaid";

	@Column(name = "PaidAt")
	private LocalDateTime paidAt;

	@Column(name = "TransactionID", length = 255)
	private String transactionID;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ShippingProviderID")
	private ShippingProvider shippingProvider;

	@Column(name = "ShippingAddress", nullable = false, length = 500)
	private String shippingAddress;

	@Column(name = "RecipientName", nullable = false, length = 255)
	private String recipientName;

	@Column(name = "RecipientPhone", nullable = false, length = 20)
	private String recipientPhone;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ShipperID")
	private User shipper;

	@Column(name = "Subtotal", nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "ShippingFee", nullable = false, precision = 12, scale = 2)
	private BigDecimal shippingFee = BigDecimal.ZERO;

	@Column(name = "DiscountAmount", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountAmount = BigDecimal.ZERO;

	@Column(name = "GrandTotal", nullable = false, precision = 12, scale = 2)
	private BigDecimal grandTotal;

	@Column(name = "Notes", length = 500)
	private String notes;

	@Column(name = "CancellationReason", length = 500)
	private String cancellationReason;

	@Column(name = "CompletedAt")
	private LocalDateTime completedAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderDetail> orderDetails = new ArrayList<>();
}
