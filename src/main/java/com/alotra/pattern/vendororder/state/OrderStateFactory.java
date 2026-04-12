package com.alotra.pattern.vendororder.state;

import org.springframework.stereotype.Component;

import com.alotra.entity.order.Order;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderStateFactory {
    private final PendingStateHandler pendingStateHandler;
    private final ConfirmedStateHandler confirmedStateHandler;
    private final DeliveringStateHandler deliveringStateHandler;
    private final CancelledStateHandler cancelledStateHandler;

    public OrderStateHandler getState(Order order) {
        return switch (order.getOrderStatus()) {
            case "Pending" -> pendingStateHandler;
            case "Confirmed" -> confirmedStateHandler;
            case "Delivering" -> deliveringStateHandler;
            case "Cancelled" -> cancelledStateHandler;
            default -> throw new IllegalStateException("Unsupported order status: " + order.getOrderStatus());
        };
    }
}
