//package com.alotra.entity.order;
//
//<<<<<<< HEAD
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.EqualsAndHashCode;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.HashSet;
//import java.util.Set;
//
//import com.alotra.entity.promotion.Promotion;
//import com.alotra.entity.user.Customer;
//import com.alotra.entity.user.Employee;
//
//@Entity
//@Table(name = "Orders")
//@Getter // <-- Thay thế
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@EqualsAndHashCode(of = "orderId")
//public class Order {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "OrderID")
//    private Integer orderId;
//
//    @ManyToOne
//    @JoinColumn(name = "CustomerID", nullable = false)
//    private Customer customer;
//
//    @ManyToOne
//    @JoinColumn(name = "EmployeeID")
//    private Employee employee;
//
//    @ManyToOne
//    @JoinColumn(name = "PromotionID")
//    private Promotion promotion;
//
//    @Column(name = "OrderDate", nullable = false)
//    private Instant orderDate;
//
//    @Column(name = "OrderStatus", nullable = false)
//    private String orderStatus; // 'Pending', 'Processing', 'Delivering', 'Completed', 'Cancelled'
//
//    @Column(name = "PaymentStatus", nullable = false)
//    private String paymentStatus; // 'Unpaid', 'Paid'
//
//    @Column(name = "PaymentMethod", nullable = false)
//    private String paymentMethod; // 'Cash', 'Momo', 'VNPay', 'ZaloPay', 'BankTransfer'
//
//    @Column(name = "PaidAt")
//    private Instant paidAt;
//
//    @Column(name = "ShippingAddress", nullable = false)
//    private String shippingAddress;
//
//    @Column(name = "RecipientName", nullable = false)
//    private String recipientName;
//
//    @Column(name = "RecipientPhone", nullable = false)
//    private String recipientPhone;
//
//    @Column(name = "Subtotal", nullable = false)
//    private BigDecimal subtotal;
//
//    @Column(name = "DiscountAmount", nullable = false)
//    private BigDecimal discountAmount;
//
//    @Column(name = "ShippingFee", nullable = false)
//    private BigDecimal shippingFee;
//
//    @Column(name = "GrandTotal", nullable = false)
//    private BigDecimal grandTotal;
//
//    @Column(name = "Notes")
//    private String notes;
//    
//    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//    private Set<OrderItem> items = new HashSet<>();
//
//    // === Thêm hàm trợ giúp (không bắt buộc nhưng nên có) ===
//    public void addItem(OrderItem item) {
//        items.add(item);
//        item.setOrder(this);
//    }
//=======
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//import com.alotra.entity.location.ShippingProvider;
//import com.alotra.entity.promotion.Promotion;
//import com.alotra.entity.shop.Shop;
//import com.alotra.entity.user.User;
//
//import jakarta.persistence.CascadeType;
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.OneToMany;
//import jakarta.persistence.Table;
//import jakarta.persistence.Index;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.ToString;
//
//@Entity
//@Table(name = "Orders", indexes = { @Index(name = "IX_Orders_UserID", columnList = "UserID"),
//		@Index(name = "IX_Orders_ShopID", columnList = "ShopID"),
//		@Index(name = "IX_Orders_OrderStatus", columnList = "OrderStatus") })
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@ToString(exclude = { "user", "shop", "promotion", "shipper", "orderDetails" })
//public class Order {
//
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@Column(name = "OrderID")
//	private Integer orderID;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "UserID", nullable = false)
//	private User user;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "ShopID", nullable = false)
//	private Shop shop;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "PromotionID")
//	private Promotion promotion;
//
//	@Column(name = "OrderDate", nullable = false)
//	private LocalDateTime orderDate = LocalDateTime.now();
//
//	@Column(name = "OrderStatus", nullable = false, length = 30)
//	private String orderStatus = "Pending";
//
//	@Column(name = "PaymentMethod", nullable = false, length = 50)
//	private String paymentMethod;
//
//	@Column(name = "PaymentStatus", nullable = false, length = 30)
//	private String paymentStatus = "Unpaid";
//
//	@Column(name = "PaidAt")
//	private LocalDateTime paidAt;
//
//	@Column(name = "TransactionID", length = 255)
//	private String transactionID;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "ShippingProviderID")
//	private ShippingProvider shippingProvider;
//
//	@Column(name = "ShippingAddress", nullable = false, length = 500)
//	private String shippingAddress;
//
//	@Column(name = "RecipientName", nullable = false, length = 255)
//	private String recipientName;
//
//	@Column(name = "RecipientPhone", nullable = false, length = 20)
//	private String recipientPhone;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "ShipperID")
//	private User shipper;
//
//	@Column(name = "Subtotal", nullable = false, precision = 12, scale = 2)
//	private BigDecimal subtotal;
//
//	@Column(name = "ShippingFee", nullable = false, precision = 12, scale = 2)
//	private BigDecimal shippingFee = BigDecimal.ZERO;
//
//	@Column(name = "DiscountAmount", nullable = false, precision = 12, scale = 2)
//	private BigDecimal discountAmount = BigDecimal.ZERO;
//
//	@Column(name = "GrandTotal", nullable = false, precision = 12, scale = 2)
//	private BigDecimal grandTotal;
//
//	@Column(name = "Notes", length = 500)
//	private String notes;
//
//	@Column(name = "CancellationReason", length = 500)
//	private String cancellationReason;
//
//	@Column(name = "CompletedAt")
//	private LocalDateTime completedAt;
//
//	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
//	private List<OrderDetail> orderDetails = new ArrayList<>();
//>>>>>>> lam
//}



package com.alotra.entity.order; // Giữ package này

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List; // Sử dụng List thay vì Set cho orderDetails

import com.alotra.entity.location.ShippingProvider; // Import ShippingProvider
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;

import jakarta.persistence.*; // Import đầy đủ JPA annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

@Entity
@Table(name = "Orders", indexes = { // Giữ lại indexes từ nhánh lam, khớp DB
    @Index(name = "IX_Orders_UserID", columnList = "UserID"),
    @Index(name = "IX_Orders_ShopID", columnList = "ShopID"),
    @Index(name = "IX_Orders_OrderStatus", columnList = "OrderStatus"),
    @Index(name = "IX_Orders_ShipperID", columnList = "ShipperID") // Thêm index cho ShipperID nếu cần
})
@Data
@NoArgsConstructor
@AllArgsConstructor
// Thêm Excludes cho các quan hệ LAZY
@ToString(exclude = { "user", "shop", "promotion", "shippingProvider", "shipper", "orderDetails" })
@EqualsAndHashCode(exclude = { "user", "shop", "promotion", "shippingProvider", "shipper", "orderDetails" }) // Exclude để tránh lỗi vòng lặp
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID") // Khớp DB
    private Integer orderID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false) // Khớp DB
    private User user; // Khách hàng

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopID", nullable = false) // Khớp DB
    private Shop shop; // Cửa hàng xử lý đơn

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PromotionID") // Khớp DB
    private Promotion promotion; // Khuyến mãi áp dụng (nếu có)

    @Column(name = "OrderDate", nullable = false, updatable = false) // Khớp DB, thêm updatable=false
    private LocalDateTime orderDate;

    @Column(name = "OrderStatus", nullable = false, length = 30) // Khớp DB
    private String orderStatus; // Giá trị mặc định 'Pending' sẽ tốt hơn nếu set ở @PrePersist hoặc Service

    @Column(name = "PaymentMethod", nullable = false, length = 50) // Khớp DB
    private String paymentMethod;

    @Column(name = "PaymentStatus", nullable = false, length = 30) // Khớp DB
    private String paymentStatus; // Giá trị mặc định 'Unpaid' sẽ tốt hơn nếu set ở @PrePersist hoặc Service

    @Column(name = "PaidAt") // Khớp DB
    private LocalDateTime paidAt;

    @Column(name = "TransactionID", length = 255) // Khớp DB
    private String transactionID; // ID giao dịch từ cổng thanh toán

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShippingProviderID") // Khớp DB
    private ShippingProvider shippingProvider; // Đơn vị vận chuyển

    @Column(name = "ShippingAddress", nullable = false, length = 500) // Khớp DB
    private String shippingAddress;

    @Column(name = "RecipientName", nullable = false, length = 255) // Khớp DB
    private String recipientName;

    @Column(name = "RecipientPhone", nullable = false, length = 20) // Khớp DB
    private String recipientPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShipperID") // Khớp DB
    private User shipper; // Nhân viên giao hàng (cũng là User)

    @Column(name = "Subtotal", nullable = false, precision = 12, scale = 2) // Khớp DB
    private BigDecimal subtotal; // Tổng tiền hàng (chưa gồm ship, giảm giá)

    @Column(name = "ShippingFee", nullable = false, precision = 12, scale = 2) // Khớp DB
    private BigDecimal shippingFee = BigDecimal.ZERO; // Phí vận chuyển

    @Column(name = "DiscountAmount", nullable = false, precision = 12, scale = 2) // Khớp DB
    private BigDecimal discountAmount = BigDecimal.ZERO; // Số tiền giảm giá

    @Column(name = "GrandTotal", nullable = false, precision = 12, scale = 2) // Khớp DB
    private BigDecimal grandTotal; // Tổng tiền cuối cùng khách phải trả

    @Column(name = "Notes", length = 500) // Khớp DB
    private String notes; // Ghi chú của khách hàng

    @Column(name = "CancellationReason", length = 500) // Khớp DB
    private String cancellationReason; // Lý do hủy đơn (nếu có)

    @Column(name = "CompletedAt") // Khớp DB
    private LocalDateTime completedAt; // Thời gian hoàn thành đơn

    // Sử dụng List và OrderDetail từ nhánh lam, khớp DB
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // Thêm LAZY
    private List<OrderDetail> orderDetails = new ArrayList<>();

    // Tự động gán giá trị mặc định khi tạo mới
    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        if (orderStatus == null) {
            orderStatus = "Pending";
        }
        if (paymentStatus == null) {
            paymentStatus = "Unpaid";
        }
        if (shippingFee == null) {
            shippingFee = BigDecimal.ZERO;
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
    }
}