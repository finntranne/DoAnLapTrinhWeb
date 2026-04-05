package com.alotra.pattern.vendororder.command;

import com.alotra.pattern.vendororder.context.VendorOrderContext;

public interface OrderActionCommand {
    void execute(VendorOrderContext ctx);
}
