package com.alotra.pattern.vendororder.strategy;

import com.alotra.entity.user.User;

public interface ShipperSelectionStrategy {
    ShipperSelectionType getType();

    User select(ShipperSelectionContext context);
}
