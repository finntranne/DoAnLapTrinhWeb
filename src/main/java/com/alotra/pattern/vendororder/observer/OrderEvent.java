package com.alotra.pattern.vendororder.observer;

import com.alotra.entity.order.Order;
import com.alotra.entity.user.User;
import lombok.Getter;

@Getter
public abstract class OrderEvent {
    private final Order order;
    private final User actor;
    private final String oldStatus;
    private final String newStatus;
    private final String note;

    protected OrderEvent(Order order, User actor, String oldStatus, String newStatus, String note) {
        this.order = order;
        this.actor = actor;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.note = note;
    }
}
