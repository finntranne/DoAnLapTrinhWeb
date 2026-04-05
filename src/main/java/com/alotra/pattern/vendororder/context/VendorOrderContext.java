package com.alotra.pattern.vendororder.context;

import com.alotra.entity.order.Order;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.pattern.vendororder.observer.OrderEventPublisher;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VendorOrderContext {
    private final Order order;
    private final User actor;
    private final User shipper;
    private final Shop shop;
    private final String note;
    private final OrderEventPublisher eventPublisher;
}
