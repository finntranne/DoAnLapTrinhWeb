package com.alotra.pattern.vendororder.command;

import org.springframework.stereotype.Component;

import com.alotra.pattern.vendororder.context.VendorOrderContext;
import com.alotra.pattern.vendororder.state.OrderStateFactory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CancelOrderCommand implements OrderActionCommand {
    private final OrderStateFactory orderStateFactory;

    @Override
    public void execute(VendorOrderContext ctx) {
        orderStateFactory.getState(ctx.getOrder()).cancel(ctx);
    }
}
