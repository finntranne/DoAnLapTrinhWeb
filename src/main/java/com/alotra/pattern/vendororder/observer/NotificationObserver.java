package com.alotra.pattern.vendororder.observer;

import org.springframework.stereotype.Component;

import com.alotra.service.notification.NotificationService;
import com.alotra.util.OrderPricingUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationObserver implements OrderEventObserver {

    private final NotificationService notificationService;

    @Override
    public void update(OrderEvent event) {
        if (event instanceof OrderAssignedEvent assignedEvent) {
            notificationService.notifyCustomerAboutOrderAssigned(
                    event.getOrder().getUser().getId(),
                    event.getOrder().getOrderID());

            if (assignedEvent.getShipper() != null) {
                notificationService.notifyShipperAboutAssignment(
                        assignedEvent.getShipper().getId(),
                        event.getOrder().getOrderID(),
                        OrderPricingUtils.formatAddress(event.getOrder().getAddress()));
            }
            return;
        }

        notificationService.notifyCustomerAboutOrderStatus(
                event.getOrder().getUser().getId(),
                event.getOrder().getOrderID(),
                event.getNewStatus());
    }
}
