package com.alotra.entity.order;

import com.alotra.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "OrderShippingHistory", indexes = {
    @Index(name = "IX_OrderShippingHistory_OrderID", columnList = "OrderID"),
    @Index(name = "IX_OrderShippingHistory_ShipperID", columnList = "ShipperID"),
    @Index(name = "IX_OrderShippingHistory_Status", columnList = "Status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"order", "shipper"})
public class OrderShippingHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShippingHistoryID")
    private Integer shippingHistoryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShipperID", nullable = false)
    private User shipper;
    
    @Column(name = "Status", nullable = false, length = 30)
    private String status; // Assigned, Picking_Up, Delivering, Delivery_Attempt, Delivered, Failed_Delivery
    
    @Column(name = "Timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "Notes", length = 500)
    private String notes;
    
    @Column(name = "Location", length = 500)
    private String location;
    
    @Column(name = "ImageURL", length = 500)
    private String imageURL; // Ảnh chứng minh giao hàng
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}