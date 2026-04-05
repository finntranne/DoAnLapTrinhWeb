package com.alotra.pattern.vendororder.state;

import com.alotra.pattern.vendororder.context.VendorOrderContext;

public interface OrderStateHandler {
    void confirm(VendorOrderContext ctx);
    void assignShipper(VendorOrderContext ctx);
    void cancel(VendorOrderContext ctx);
}
