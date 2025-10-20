package com.alotra.repository.cart;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.cart.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    
    Optional<Cart> findByUser_UserID(Integer userId);
}
