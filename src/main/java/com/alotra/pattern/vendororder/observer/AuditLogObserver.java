package com.alotra.pattern.vendororder.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogObserver implements OrderEventObserver {

    private static final Logger log = LoggerFactory.getLogger(AuditLogObserver.class);

    @Override
    public void update(OrderEvent event) {
        log.info("Order event: orderId={}, oldStatus={}, newStatus={}, actorId={}, note={}",
                event.getOrder().getOrderID(),
                event.getOldStatus(),
                event.getNewStatus(),
                event.getActor() != null ? event.getActor().getId() : null,
                event.getNote());
    }
}
