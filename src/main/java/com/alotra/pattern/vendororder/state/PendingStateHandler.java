package com.alotra.pattern.vendororder.state;

import org.springframework.stereotype.Component;

import com.alotra.pattern.vendororder.context.VendorOrderContext;
import com.alotra.pattern.vendororder.observer.OrderCancelledEvent;
import com.alotra.pattern.vendororder.observer.OrderConfirmedEvent;

@Component
public class PendingStateHandler extends AbstractOrderStateHandler {
    @Override
    public void confirm(VendorOrderContext ctx) {
        String oldStatus = ctx.getOrder().getOrderStatus();
        ctx.getOrder().setOrderStatus("Confirmed");
        ctx.getEventPublisher().publish(new OrderConfirmedEvent(
                ctx.getOrder(), ctx.getActor(), oldStatus, "Confirmed",
                ctx.getNote() != null ? ctx.getNote() : "Xac nhan don hang"));
    }

    @Override
    public void cancel(VendorOrderContext ctx) {
        String oldStatus = ctx.getOrder().getOrderStatus();
        ctx.getOrder().setOrderStatus("Cancelled");
        ctx.getEventPublisher().publish(new OrderCancelledEvent(
                ctx.getOrder(), ctx.getActor(), oldStatus, "Cancelled",
                ctx.getNote() != null ? ctx.getNote() : "Huy don hang"));
    }
}
