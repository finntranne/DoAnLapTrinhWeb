package com.alotra.entity.order;

import java.time.LocalDateTime;

import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AuditLogs", indexes = {
        @Index(name = "IX_AuditLogs_ShopID_CreatedAt", columnList = "ShopID, CreatedAt"),
        @Index(name = "IX_AuditLogs_OrderID_CreatedAt", columnList = "OrderID, CreatedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AuditLogID")
    private Integer auditLogID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopID", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ActorID")
    private User actor;

    @Column(name = "EventType", nullable = false, length = 50)
    private String eventType;

    @Column(name = "OldStatus", length = 30)
    private String oldStatus;

    @Column(name = "NewStatus", length = 30)
    private String newStatus;

    @Column(name = "Note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
