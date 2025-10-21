package com.alotra.entity.cart;

import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Topping; // <-- Import Topping
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.Set;    // <-- Import Set
import java.util.HashSet;// <-- Import HashSet

@Entity
@Table(name = "CartItems") // Bảng lưu các món hàng trong tất cả giỏ hàng
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CartItemID")
    private Integer cartItemId; // ID tự tăng

    // Mỗi món hàng thuộc về một giỏ hàng (Cart)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CartID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Cart cart;

    // Mỗi món hàng tương ứng với một biến thể sản phẩm (size + giá gốc)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ProductVariant variant;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity; // Số lượng

    // === QUAN HỆ VỚI TOPPING ĐÃ CHỌN ===
    // Một món hàng có thể có nhiều topping được chọn
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "CartItemToppings", // Bảng trung gian lưu topping đã chọn cho món hàng
        joinColumns = @JoinColumn(name = "CartItemID"),
        inverseJoinColumns = @JoinColumn(name = "ToppingID")
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Topping> selectedToppings = new HashSet<>();
    // ===================================
}