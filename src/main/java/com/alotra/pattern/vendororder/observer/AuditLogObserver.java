package com.alotra.pattern.vendororder.observer;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alotra.entity.order.AuditLog;
import com.alotra.repository.order.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditLogObserver implements OrderEventObserver {

    private static final Logger log = LoggerFactory.getLogger(AuditLogObserver.class);

    private final AuditLogRepository auditLogRepository;

    @Override
    public void update(OrderEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOrder(event.getOrder());
        auditLog.setShop(event.getOrder().getShop());
        auditLog.setActor(event.getActor());
        auditLog.setEventType(event.getClass().getSimpleName());
        auditLog.setOldStatus(event.getOldStatus());
        auditLog.setNewStatus(event.getNewStatus());
        auditLog.setNote(event.getNote());
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(auditLog);

        log.info("Audit log saved: orderId={}, eventType={}, oldStatus={}, newStatus={}, actorId={}",
                event.getOrder().getOrderID(),
                auditLog.getEventType(),
                event.getOldStatus(),
                event.getNewStatus(),
                event.getActor() != null ? event.getActor().getId() : null);
    }
}
