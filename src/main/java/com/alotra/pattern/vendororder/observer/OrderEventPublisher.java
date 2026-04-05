package com.alotra.pattern.vendororder.observer;

import java.util.ArrayList;
import java.util.List;

public class OrderEventPublisher {
    private final List<OrderEventObserver> observers = new ArrayList<>();

    public void publish(OrderEvent event) {
        observers.forEach(observer -> observer.update(event));
    }

    public void addObserver(OrderEventObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(OrderEventObserver observer) {
        observers.remove(observer);
    }
}
