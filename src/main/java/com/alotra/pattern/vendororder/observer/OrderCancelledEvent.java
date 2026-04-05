package com.alotra.pattern.vendororder.observer;

import com.alotra.entity.order.Order;
import com.alotra.entity.user.User;

public class OrderCancelledEvent extends OrderEvent {
    public OrderCancelledEvent(Order order, User actor, String oldStatus, String newStatus, String note) {
        super(order, actor, oldStatus, newStatus, note);
    }
}
