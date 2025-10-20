package com.alotra.entity.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

import com.alotra.entity.user.User;

@Entity
@Table(name = "OrderHistory", indexes = {
    @Index(name = "IX_OrderHistory_OrderID", columnList = "OrderID")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"order", "changedByUser"})
public class OrderHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryID")
    private Integer historyID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;
    
    @Column(name = "OldStatus", length = 30)
    private String oldStatus;
    
    @Column(name = "NewStatus", nullable = false, length = 30)
    private String newStatus;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ChangedByUserID")
    private User changedByUser;
    
    @Column(name = "Timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(name = "Notes", length = 500)
    private String notes;
}