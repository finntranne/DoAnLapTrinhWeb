package com.alotra.pattern.vendororder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alotra.pattern.vendororder.observer.AuditLogObserver;
import com.alotra.pattern.vendororder.observer.NotificationObserver;
import com.alotra.pattern.vendororder.observer.OrderEventPublisher;
import com.alotra.pattern.vendororder.observer.OrderHistoryObserver;

@Configuration
public class VendorOrderPatternConfig {

    @Bean
    public OrderEventPublisher orderEventPublisher(
            NotificationObserver notificationObserver,
            OrderHistoryObserver orderHistoryObserver,
            AuditLogObserver auditLogObserver) {
        OrderEventPublisher publisher = new OrderEventPublisher();
        publisher.addObserver(notificationObserver);
        publisher.addObserver(orderHistoryObserver);
        publisher.addObserver(auditLogObserver);
        return publisher;
    }
}
