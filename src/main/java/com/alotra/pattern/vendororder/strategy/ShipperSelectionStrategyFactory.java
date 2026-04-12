package com.alotra.pattern.vendororder.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ShipperSelectionStrategyFactory {

    private final Map<ShipperSelectionType, ShipperSelectionStrategy> strategies;

    public ShipperSelectionStrategyFactory(List<ShipperSelectionStrategy> strategies) {
        this.strategies = new EnumMap<>(ShipperSelectionType.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.getType(), strategy));
    }

    public ShipperSelectionStrategy getStrategy(ShipperSelectionType type) {
        ShipperSelectionStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("Unsupported shipper selection type: " + type);
        }
        return strategy;
    }
}
