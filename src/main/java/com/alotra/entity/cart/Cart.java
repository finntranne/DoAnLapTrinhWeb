

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
