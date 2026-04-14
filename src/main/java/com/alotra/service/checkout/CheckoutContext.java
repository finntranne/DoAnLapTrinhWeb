package com.alotra.service.checkout;

import java.util.List;

import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.location.Address;
import com.alotra.entity.order.Order;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.enums.PaymentMethod;

public class CheckoutContext {
    private User user;
    private Cart cart;
    private List<CartItem> itemsToOrder;
    private Integer addressId;
    private Address chosenAddress;
    private Address orderAddress;
    private Shop shop;
    private String notes;
    private PaymentMethod paymentMethod;
    private Order order;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public List<CartItem> getItemsToOrder() {
        return itemsToOrder;
    }

    public void setItemsToOrder(List<CartItem> itemsToOrder) {
        this.itemsToOrder = itemsToOrder;
    }

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    public Address getChosenAddress() {
        return chosenAddress;
    }

    public void setChosenAddress(Address chosenAddress) {
        this.chosenAddress = chosenAddress;
    }

    public Address getOrderAddress() {
        return orderAddress;
    }

    public void setOrderAddress(Address orderAddress) {
        this.orderAddress = orderAddress;
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}

