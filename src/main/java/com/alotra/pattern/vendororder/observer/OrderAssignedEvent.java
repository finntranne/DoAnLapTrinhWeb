package com.alotra.pattern.vendororder.observer;

import com.alotra.entity.order.Order;
import com.alotra.entity.user.User;
import lombok.Getter;

@Getter
public class OrderAssignedEvent extends OrderEvent {
    private final User shipper;

    public OrderAssignedEvent(Order order, User actor, User shipper, String oldStatus, String newStatus, String note) {
        super(order, actor, oldStatus, newStatus, note);
        this.shipper = shipper;
    }
}
