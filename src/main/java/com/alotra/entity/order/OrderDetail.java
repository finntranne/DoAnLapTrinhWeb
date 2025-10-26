//package com.alotra.entity.order;
//
//<<<<<<< HEAD
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.math.BigDecimal;
//
//import com.alotra.entity.product.ProductVariant;
//=======
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//
//import com.alotra.entity.product.ProductVariant;
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
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.ToString;
//>>>>>>> lam
//
//@Entity
//@Table(name = "OrderDetails")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//<<<<<<< HEAD
//public class OrderDetail {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "OrderDetailID")
//    private Integer orderDetailId;
//
//    @ManyToOne
//    @JoinColumn(name = "OrderID", nullable = false)
//    private Order order;
//
//    @ManyToOne
//    @JoinColumn(name = "VariantID", nullable = false)
//    private ProductVariant variant;
//
//    @Column(name = "Quantity", nullable = false)
//    private Integer quantity;
//
//    @Column(name = "UnitPrice", nullable = false)
//    private BigDecimal unitPrice;
//
//    @Column(name = "LineDiscount", nullable = false)
//    private BigDecimal lineDiscount;
//
//    @Column(name = "LineTotal", nullable = false)
//    private BigDecimal lineTotal;
//=======
//@ToString(exclude = {"order", "variant"})
//public class OrderDetail {
//    
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "OrderDetailID")
//    private Integer orderDetailID;
//    
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "OrderID", nullable = false)
//    private Order order;
//    
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "VariantID", nullable = false)
//    private ProductVariant variant;
//    
//    @Column(name = "Quantity", nullable = false)
//    private Integer quantity;
//    
//    @Column(name = "UnitPrice", nullable = false, precision = 10, scale = 2)
//    private BigDecimal unitPrice;
//    
//    @Column(name = "Subtotal", nullable = false, precision = 12, scale = 2)
//    private BigDecimal subtotal;
//    
//    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL)
//    private List<OrderDetailTopping> toppings = new ArrayList<>();
//>>>>>>> lam
//}


package com.alotra.entity.order; // Giữ package này

import java.math.BigDecimal;
import java.util.ArrayList; // Import ArrayList
import java.util.List;    // Import List

import com.alotra.entity.product.ProductVariant;

import jakarta.persistence.*; // Import đầy đủ JPA annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

@Entity
@Table(name = "OrderDetails") // Khớp DB
@Data
@NoArgsConstructor
@AllArgsConstructor
// Thêm Excludes cho các quan hệ
@ToString(exclude = {"order", "variant", "toppings"})
@EqualsAndHashCode(exclude = {"order", "variant", "toppings"}) // Exclude để tránh lỗi vòng lặp
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderDetailID") // Khớp DB
    private Integer orderDetailID;

    @ManyToOne(fetch = FetchType.LAZY) // Thêm LAZY
    @JoinColumn(name = "OrderID", nullable = false) // Khớp DB
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY) // Thêm LAZY
    @JoinColumn(name = "VariantID", nullable = false) // Khớp DB
    private ProductVariant variant;

    @Column(name = "Quantity", nullable = false) // Khớp DB
    private Integer quantity;

    // UnitPrice là giá của riêng variant tại thời điểm mua
    @Column(name = "UnitPrice", nullable = false, precision = 10, scale = 2) // Khớp DB
    private BigDecimal unitPrice;

    // Subtotal là (Quantity * UnitPrice) + (Tổng giá toppings) tại thời điểm mua
    @Column(name = "Subtotal", nullable = false, precision = 12, scale = 2) // Khớp DB
    private BigDecimal subtotal;

    // Giữ lại quan hệ với OrderDetailTopping từ nhánh lam, khớp DB
    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // Thêm orphanRemoval và LAZY
    private List<OrderDetailTopping> toppings = new ArrayList<>();

    // Không cần các trường lineDiscount, lineTotal từ HEAD vì subtotal đã bao gồm ý nghĩa đó
}