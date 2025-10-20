package com.alotra.entity.promotion;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

import com.alotra.entity.user.User;

@Entity
@Table(name = "PromotionApprovals", indexes = {
    @Index(name = "IX_PromotionApprovals_PromotionID", columnList = "PromotionID"),
    @Index(name = "IX_PromotionApprovals_Status", columnList = "Status"),
    @Index(name = "IX_PromotionApprovals_RequestedBy", columnList = "RequestedByUserID"),
    @Index(name = "IX_PromotionApprovals_RequestedAt", columnList = "RequestedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"promotion", "requestedBy", "reviewedBy"})
public class PromotionApproval {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ApprovalID")
    private Integer approvalId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PromotionID", nullable = true)
    private Promotion promotion;
    
    @Column(name = "ActionType", nullable = false, length = 20)
    private String actionType;
    
    @Column(name = "Status", nullable = false, length = 20)
    private String status = "Pending";
    
    @Column(name = "ChangeDetails", columnDefinition = "NVARCHAR(MAX)")
    private String changeDetails;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequestedByUserID", nullable = false)
    private User requestedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReviewedByUserID", nullable = true)
    private User reviewedBy;
    
    @Column(name = "RejectionReason", length = 500)
    private String rejectionReason;
    
    @Column(name = "RequestedAt", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();
    
    @Column(name = "ReviewedAt")
    private LocalDateTime reviewedAt;
}