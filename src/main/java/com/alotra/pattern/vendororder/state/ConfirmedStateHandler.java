package com.alotra.pattern.vendororder.state;

import org.springframework.stereotype.Component;

import com.alotra.pattern.vendororder.context.VendorOrderContext;
import com.alotra.pattern.vendororder.observer.OrderAssignedEvent;
import com.alotra.pattern.vendororder.observer.OrderCancelledEvent;

@Component
public class ConfirmedStateHandler extends AbstractOrderStateHandler {
    @Override
    public void assignShipper(VendorOrderContext ctx) {
        String oldStatus = ctx.getOrder().getOrderStatus();
        ctx.getOrder().setShipper(ctx.getShipper());
        ctx.getOrder().setOrderStatus("Delivering");
        ctx.getEventPublisher().publish(new OrderAssignedEvent(
                ctx.getOrder(), ctx.getActor(), ctx.getShipper(), oldStatus, "Delivering",
                ctx.getNote() != null ? ctx.getNote() : "Gan shipper cho don hang"));
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
