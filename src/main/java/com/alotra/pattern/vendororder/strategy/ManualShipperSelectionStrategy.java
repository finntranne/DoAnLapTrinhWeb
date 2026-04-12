package com.alotra.pattern.vendororder.strategy;

import org.springframework.stereotype.Component;

import com.alotra.entity.user.User;

@Component
public class ManualShipperSelectionStrategy implements ShipperSelectionStrategy {

    @Override
    public ShipperSelectionType getType() {
        return ShipperSelectionType.MANUAL;
    }

    @Override
    public User select(ShipperSelectionContext context) {
        Integer preferredShipperId = context.getPreferredShipperId();
        if (preferredShipperId == null) {
            throw new IllegalStateException("Manual selection requires a shipper");
        }

        return context.getCandidates().stream()
                .filter(candidate -> preferredShipperId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Selected shipper is not available"));
    }
}
