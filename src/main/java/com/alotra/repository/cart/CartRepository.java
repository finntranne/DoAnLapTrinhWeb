package com.alotra.repository.cart; // Hoặc package repository của bạn

import com.alotra.entity.cart.Cart;
import com.alotra.entity.user.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    // Tìm giỏ hàng theo khách hàng
    Optional<Cart> findByCustomer(Customer customer);
}