package com.alotra.service.vendor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.order.AuditLog;
import com.alotra.repository.order.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VendorAuditLogService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLog> getAuditLogs(Integer shopId, Integer orderId, Pageable pageable) {
        if (orderId != null) {
            return auditLogRepository.findByShop_ShopIdAndOrder_OrderIDOrderByCreatedAtDesc(shopId, orderId, pageable);
        }
        return auditLogRepository.findByShop_ShopIdOrderByCreatedAtDesc(shopId, pageable);
    }
}
