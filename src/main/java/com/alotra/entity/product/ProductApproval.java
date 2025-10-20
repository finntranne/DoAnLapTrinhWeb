package com.alotra.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.alotra.entity.user.User;

@Entity
@Table(name = "ProductApprovals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductApproval {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer approvalId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID")
    private Product product;
    
    @Column(nullable = false, length = 20)
    private String actionType; // CREATE, UPDATE, DELETE
    
    @Column(nullable = false, length = 20)
    private String status = "Pending"; // Pending, Approved, Rejected
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String changeDetails; // JSON string chứa chi tiết thay đổi
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequestedByUserID", nullable = false)
    private User requestedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReviewedByUserID")
    private User reviewedBy;
    
    @Column(columnDefinition = "NVARCHAR(500)")
    private String rejectionReason;
    
    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();
    
    private LocalDateTime reviewedAt;
    
    // Getters and Setters được tự động tạo bởi Lombok
}