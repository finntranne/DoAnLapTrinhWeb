package com.alotra.pattern.vendororder.observer;

public interface OrderEventObserver {
    void update(OrderEvent event);
}
