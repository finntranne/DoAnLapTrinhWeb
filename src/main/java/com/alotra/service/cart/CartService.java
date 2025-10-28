package com.alotra.service.cart; // Hoặc package service của bạn

import com.alotra.entity.cart.CartItem;
import com.alotra.entity.user.User;
import com.alotra.repository.cart.CartItemRepository;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.product.ProductVariantRepository;
import com.alotra.repository.product.ToppingRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public interface CartService {
    BigDecimal calculateSubtotal(Set<CartItem> items);
  
    void clearCart(User user);

	BigDecimal getSubtotal(User user);

	int getCartItemCount(User user);

	CartItem updateItemQuantity(User user, Integer cartItemId, int newQuantity);

	void removeItemFromCart(User user, Integer cartItemId);

	CartItem addItemToCart(User user, Integer variantId, int quantity, List<Integer> toppingIds);
	
	public BigDecimal getLineTotal(CartItem item);
}