package com.alotra.service.cart; // Hoặc package service của bạn

import com.alotra.entity.cart.CartItem;
import com.alotra.entity.user.Customer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface CartService {
    // Hàm thêm món hàng vào giỏ
    void addItemToCart(Customer customer, Integer variantId, int quantity, List<Integer> toppingIds);
    
    BigDecimal calculateSubtotal(Set<CartItem> items);
    
    void removeItemFromCart(Customer customer, Long cartItemId);
    
    CartItem updateItemQuantity(Customer customer, Long cartItemId, int newQuantity);
    
    int getCartItemCount(Customer customer);
    
    BigDecimal getSubtotal(Customer customer);
    
    void clearCart(Customer customer);
}