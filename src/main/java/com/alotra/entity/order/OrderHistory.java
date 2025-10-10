package com.alotra.entity.order;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "OrderHistory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryID")
    private Integer historyId;

    @ManyToOne
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    @Column(name = "PreviousStatus")
    private String previousStatus;

    @Column(name = "NewStatus", nullable = false)
    private String newStatus;

    @Column(name = "ChangedBy")
    private Integer changedBy;

    @Column(name = "UserType", nullable = false)
    private String userType; // 'Employee', 'Customer', 'System'

    @Column(name = "Timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "Notes")
    private String notes;
}
