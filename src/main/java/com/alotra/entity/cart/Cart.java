package com.alotra.entity.cart; // Hoặc package entity của bạn

import com.alotra.entity.user.Customer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Carts") // Bảng lưu các giỏ hàng
@Data
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CartID")
    private Integer cartId; // ID tự tăng của giỏ hàng

    // Mỗi giỏ hàng thuộc về một khách hàng duy nhất
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID", nullable = false, unique = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Customer customer;

    // Một giỏ hàng có nhiều món hàng (CartItem)
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<CartItem> items = new HashSet<>();

    // Hàm tiện ích để thêm món hàng vào giỏ
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    // Hàm tiện ích để xóa món hàng khỏi giỏ
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }
}