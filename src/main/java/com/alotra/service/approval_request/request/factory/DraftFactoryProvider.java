package com.alotra.service.approval_request.request.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.alotra.enums.TargetType;

@Component
public class DraftFactoryProvider {

    private final Map<TargetType, DraftAbstractFactory<?>> factoryMap;

    public DraftFactoryProvider(List<DraftAbstractFactory<?>> factories) {

        factoryMap = new HashMap<>();

        for (DraftAbstractFactory<?> factory : factories) {
            if (factory instanceof ProductDraftFactory) {
                factoryMap.put(TargetType.PRODUCT, factory);
            } else if (factory instanceof ToppingDraftFactory) {
                factoryMap.put(TargetType.TOPPING, factory);
            } else if (factory instanceof PromotionDraftFactory) {
                factoryMap.put(TargetType.PROMOTION, factory);
            }
        }
    }

    public DraftAbstractFactory<?> getFactory(TargetType type) {
        return factoryMap.get(type);
    }
}
