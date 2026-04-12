package com.alotra.pattern.vendororder.state;

import org.springframework.stereotype.Component;

import com.alotra.pattern.vendororder.context.VendorOrderContext;
import com.alotra.pattern.vendororder.observer.OrderAssignedEvent;

@Component
public class DeliveringStateHandler extends AbstractOrderStateHandler {
    @Override
    public void assignShipper(VendorOrderContext ctx) {
        String oldStatus = ctx.getOrder().getOrderStatus();
        ctx.getOrder().setShipper(ctx.getShipper());
        ctx.getEventPublisher().publish(new OrderAssignedEvent(
                ctx.getOrder(), ctx.getActor(), ctx.getShipper(), oldStatus, oldStatus,
                ctx.getNote() != null ? ctx.getNote() : "Thay doi shipper"));
    }
}
