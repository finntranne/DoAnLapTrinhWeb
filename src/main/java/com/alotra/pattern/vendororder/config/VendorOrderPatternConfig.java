package com.alotra.pattern.vendororder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alotra.pattern.vendororder.observer.AuditLogObserver;
import com.alotra.pattern.vendororder.observer.NotificationObserver;
import com.alotra.pattern.vendororder.observer.OrderEventPublisher;

@Configuration
public class VendorOrderPatternConfig {

    @Bean
    public OrderEventPublisher orderEventPublisher(
            NotificationObserver notificationObserver,
            AuditLogObserver auditLogObserver) {
        OrderEventPublisher publisher = new OrderEventPublisher();
        publisher.addObserver(notificationObserver);
        publisher.addObserver(auditLogObserver);
        return publisher;
    }
}
