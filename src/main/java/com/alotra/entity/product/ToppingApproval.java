package com.alotra.entity.product;

import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ToppingApprovals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"topping", "shop", "requestedBy", "reviewedBy"})
@EqualsAndHashCode(exclude = {"topping", "shop", "requestedBy", "reviewedBy"})
public class ToppingApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ApprovalID")
    private Integer approvalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ToppingID")
    private Topping topping;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopID", nullable = false)
    private Shop shop;

    @Column(name = "ActionType", nullable = false, length = 20)
    private String actionType; // CREATE, UPDATE, DELETE

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "Pending";

    @Column(name = "ChangeDetails", columnDefinition = "NVARCHAR(MAX)")
    private String changeDetails; // JSON

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequestedByUserID", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReviewedByUserID")
    private User reviewedBy;

    @Column(name = "RejectionReason", length = 500)
    private String rejectionReason;

    @Column(name = "RequestedAt", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "ReviewedAt")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }
}