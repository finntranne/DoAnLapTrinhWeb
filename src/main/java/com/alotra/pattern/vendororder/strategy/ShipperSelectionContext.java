package com.alotra.pattern.vendororder.strategy;

import java.util.List;

import com.alotra.entity.order.Order;
import com.alotra.entity.user.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShipperSelectionContext {
    private final Order order;
    private final Integer preferredShipperId;
    private final List<User> candidates;
}
