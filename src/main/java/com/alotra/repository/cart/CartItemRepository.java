package com.alotra.repository.cart;

import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Tìm một món hàng cụ thể trong giỏ theo biến thể (size)
    // (Chúng ta sẽ kiểm tra topping trong Service)
    Optional<CartItem> findByCartAndVariant(Cart cart, ProductVariant variant);
}