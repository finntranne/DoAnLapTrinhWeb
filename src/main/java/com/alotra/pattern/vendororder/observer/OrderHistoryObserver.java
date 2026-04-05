package com.alotra.pattern.vendororder.observer;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.alotra.entity.order.OrderHistory;
import com.alotra.entity.order.OrderShippingHistory;
import com.alotra.repository.order.OrderHistoryRepository;
import com.alotra.repository.order.OrderShippingHistoryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderHistoryObserver implements OrderEventObserver {

    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderShippingHistoryRepository orderShippingHistoryRepository;

    @Override
    public void update(OrderEvent event) {
        OrderHistory history = new OrderHistory();
        history.setOrder(event.getOrder());
        history.setOldStatus(event.getOldStatus());
        history.setNewStatus(event.getNewStatus());
        history.setChangedByUser(event.getActor());
        history.setNotes(event.getNote());
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        if (event instanceof OrderAssignedEvent assignedEvent && assignedEvent.getShipper() != null) {
            OrderShippingHistory shippingHistory = new OrderShippingHistory();
            shippingHistory.setOrder(event.getOrder());
            shippingHistory.setShipper(assignedEvent.getShipper());
            shippingHistory.setStatus("Assigned");
            shippingHistory.setNotes(event.getNote());
            shippingHistory.setTimestamp(LocalDateTime.now());
            orderShippingHistoryRepository.save(shippingHistory);
        }
    }
}
