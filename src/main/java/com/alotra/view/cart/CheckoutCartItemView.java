package com.alotra.view.cart;

import java.util.List;

import com.alotra.entity.cart.CartItem;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Topping;

public class CheckoutCartItemView {

    private final CartItem cartItem;

    public CheckoutCartItemView(CartItem cartItem) {
        this.cartItem = cartItem;
    }

    public Integer getCartItemID() {
        return cartItem.getCartItemID();
    }

    public ProductVariant getVariant() {
        return cartItem.getVariant();
    }

    public Integer getQuantity() {
        return cartItem.getQuantity();
    }

    public List<Topping> getSelectedToppings() {
        return cartItem.getToppings();
    }

    public CartItem getCartItem() {
        return cartItem;
    }
}
