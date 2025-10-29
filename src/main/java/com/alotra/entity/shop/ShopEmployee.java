package com.alotra.entity.shop;

import com.alotra.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ShopEmployees", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"UserID", "ShopID"}),
       indexes = {
           @Index(name = "IX_ShopEmployees_ShopID", columnList = "ShopID"),
           @Index(name = "IX_ShopEmployees_UserID", columnList = "UserID")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopEmployee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EmployeeID")
    private Integer employeeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopID", nullable = false)
    private Shop shop;
    
    @Column(name = "Status", nullable = false, length = 20)
    private String status = "Active"; // Active, Inactive
    
    @Column(name = "AssignedAt", nullable = false)
    private LocalDateTime assignedAt;
    
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        assignedAt = now;
        updatedAt = now;
        if (status == null) {
            status = "Active";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}