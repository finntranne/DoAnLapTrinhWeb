//package com.alotra.entity.cart; // Hoặc package entity của bạn
//
//<<<<<<< HEAD
//import com.alotra.entity.user.Customer;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.ToString;
//
//import java.util.HashSet;
//import java.util.Set;
//
//@Entity
//@Table(name = "Carts") // Bảng lưu các giỏ hàng
//@Data
//public class Cart {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "CartID")
//    private Integer cartId; // ID tự tăng của giỏ hàng
//
//    // Mỗi giỏ hàng thuộc về một khách hàng duy nhất
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "CustomerID", nullable = false, unique = true)
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Customer customer;
//
//    // Một giỏ hàng có nhiều món hàng (CartItem)
//    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Set<CartItem> items = new HashSet<>();
//
//    // Hàm tiện ích để thêm món hàng vào giỏ
//    public void addItem(CartItem item) {
//        items.add(item);
//        item.setCart(this);
//    }
//
//    // Hàm tiện ích để xóa món hàng khỏi giỏ
//    public void removeItem(CartItem item) {
//        items.remove(item);
//        item.setCart(null);
//    }
//}
//=======
//import java.time.LocalDateTime;
//
//import com.alotra.entity.user.User;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.OneToOne;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "Carts")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class Cart {
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@Column(name = "CartID")
//	private Integer cartID;
//
//	@OneToOne
//	@JoinColumn(name = "UserID", nullable = false, unique = true)
//	private User user;
//
//	@Column(name = "CreatedAt", nullable = false)
//	private LocalDateTime createdAt = LocalDateTime.now();
//
//	@Column(name = "UpdatedAt", nullable = false)
//	private LocalDateTime updatedAt = LocalDateTime.now();
//}
//>>>>>>> lam


package com.alotra.entity.cart; // Giữ package này

import java.time.LocalDateTime;
import java.util.HashSet; // Sử dụng Set từ HEAD
import java.util.Set; // Sử dụng Set từ HEAD

import com.alotra.entity.user.User; // Sử dụng User từ nhánh lam

import jakarta.persistence.*; // Import các annotation cần thiết
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Cần cho @Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Cần cho @Exclude

@Entity
@Table(name = "Carts") // Tên bảng khớp DB
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CartID") // Khớp DB và nhánh lam
    private Integer cartID;

    // Sử dụng User và JoinColumn từ nhánh lam, khớp DB
    @OneToOne(fetch = FetchType.LAZY) // Thêm LAZY fetch
    @JoinColumn(name = "UserID", nullable = false, unique = true)
    @EqualsAndHashCode.Exclude // Thêm Exclude để tránh lỗi vòng lặp
    @ToString.Exclude // Thêm Exclude để tránh lỗi vòng lặp
    private User user;

    // Lấy phần quản lý items từ nhánh HEAD, khớp DB có CartItems
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude // Thêm Exclude
    @ToString.Exclude // Thêm Exclude
    private Set<CartItem> items = new HashSet<>();

    // Giữ lại CreatedAt và UpdatedAt từ nhánh lam, khớp DB
    @Column(name = "CreatedAt", nullable = false, updatable = false) // updatable = false cho createdAt
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    // Hàm tiện ích để thêm món hàng vào giỏ (từ HEAD)
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    // Hàm tiện ích để xóa món hàng khỏi giỏ (từ HEAD)
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    // Tự động cập nhật timestamps (tốt hơn gán giá trị mặc định)
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
