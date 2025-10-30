
package com.alotra.entity.cart; // Giữ package này

import java.time.LocalDateTime;
import java.util.HashSet; // Import HashSet
import java.util.Set;    // Import Set

import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Topping; // Import Topping

import jakarta.persistence.*; // Import các annotation cần thiết
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

@Entity
@Table(name = "CartItems") // Khớp DB
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CartItemID") // Khớp DB và nhánh lam
    private Integer cartItemID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CartID", nullable = false)
    @EqualsAndHashCode.Exclude // Thêm Exclude
    @ToString.Exclude // Thêm Exclude
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantID", nullable = false)
    @EqualsAndHashCode.Exclude // Thêm Exclude
    @ToString.Exclude // Thêm Exclude
    private ProductVariant variant;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "AddedAt", nullable = false, updatable = false) // Giữ lại AddedAt từ nhánh lam, khớp DB, thêm updatable=false
    private LocalDateTime addedAt;

    // === Lấy quan hệ Topping từ nhánh HEAD, khớp bảng CartItem_Toppings ===
    @ManyToMany(fetch = FetchType.EAGER) // EAGER fetch might be acceptable for toppings in a cart item, adjust if needed
    @JoinTable(
        name = "CartItem_Toppings", // Tên bảng trung gian khớp DB
        joinColumns = @JoinColumn(name = "CartItemID"), // Khớp DB
        inverseJoinColumns = @JoinColumn(name = "ToppingID") // Khớp DB
    )
    @EqualsAndHashCode.Exclude // Thêm Exclude
    @ToString.Exclude // Thêm Exclude (Mặc dù Topping có thể không có back-ref, vẫn nên thêm)
    private Set<Topping> selectedToppings = new HashSet<>();
    // ===================================================================

    // Tự động gán AddedAt khi tạo mới
    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}