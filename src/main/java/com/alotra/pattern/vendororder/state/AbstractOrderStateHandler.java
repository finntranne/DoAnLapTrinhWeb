package com.alotra.pattern.vendororder.state;

import com.alotra.pattern.vendororder.context.VendorOrderContext;

public abstract class AbstractOrderStateHandler implements OrderStateHandler {
    @Override
    public void confirm(VendorOrderContext ctx) {
        throw new IllegalStateException("Cannot confirm order from status: " + ctx.getOrder().getOrderStatus());
    }

    @Override
    public void assignShipper(VendorOrderContext ctx) {
        throw new IllegalStateException("Cannot assign shipper from status: " + ctx.getOrder().getOrderStatus());
    }

    @Override
    public void cancel(VendorOrderContext ctx) {
        throw new IllegalStateException("Cannot cancel order from status: " + ctx.getOrder().getOrderStatus());
    }
}
