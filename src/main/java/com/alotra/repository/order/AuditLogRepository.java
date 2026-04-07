package com.alotra.repository.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.order.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    Page<AuditLog> findByShop_ShopIdOrderByCreatedAtDesc(Integer shopId, Pageable pageable);

    Page<AuditLog> findByShop_ShopIdAndOrder_OrderIDOrderByCreatedAtDesc(Integer shopId, Integer orderId, Pageable pageable);
}
