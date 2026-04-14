package com.alotra.service.checkout;

import java.util.List;

import org.springframework.stereotype.Service;

import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.order.Order;
import com.alotra.entity.user.User;
import com.alotra.enums.PaymentMethod;
import com.alotra.repository.cart.CartRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckoutFacade {

    private final CheckoutTemplate checkoutProcessor;
    private final CartRepository cartRepository;

    public Order checkoutOrder(User user, List<Integer> selectedItemIds, Integer addressId, String notes,
            PaymentMethod paymentMethod) {
        Cart cart = cartRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay gio hang"));

        List<CartItem> itemsToOrder = cart.getItems().stream()
                .filter(item -> selectedItemIds.contains(item.getCartItemID()))
                .toList();

        CheckoutContext context = new CheckoutContext();
        context.setUser(user);
        context.setCart(cart);
        context.setItemsToOrder(itemsToOrder);
        context.setAddressId(addressId);
        context.setNotes(notes);
        context.setPaymentMethod(paymentMethod);

        return checkoutProcessor.processCheckout(context).getOrder();
    }
}

