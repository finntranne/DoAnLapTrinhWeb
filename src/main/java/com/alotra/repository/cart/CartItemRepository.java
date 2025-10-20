package com.alotra.repository.cart;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.cart.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    
    List<CartItem> findByCart_CartID(Integer cartId);
    
    Optional<CartItem> findByCart_CartIDAndVariant_VariantID(Integer cartId, Integer variantId);
    
    void deleteByCart_CartID(Integer cartId);
    
    @Query("SELECT ci FROM CartItem ci " +
           "JOIN FETCH ci.variant v " +
           "JOIN FETCH v.product p " +
           "WHERE ci.cart.cartID = :cartId")
    List<CartItem> findByCartIdWithDetails(@Param("cartId") Integer cartId);
}