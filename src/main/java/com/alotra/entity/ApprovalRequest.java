package com.alotra.entity;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.alotra.entity.shop.Shop;
import com.alotra.enums.ActionType;
import com.alotra.enums.ApprovalStatus;
import com.alotra.enums.TargetType;
import com.alotra.service.approval_request.approval.ApprovalState;
import com.alotra.service.approval_request.approval.ApprovedApprovalState;
import com.alotra.service.approval_request.approval.PendingApprovalState;
import com.alotra.service.approval_request.approval.RejectedApprovalState;
import com.alotra.service.approval_request.approval.RejectedApprovalState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ApprovalRequests")
public class ApprovalRequest {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ApprovalRequestID")
	private Integer id;
	
	@Column(nullable = false)
    private Integer targetId;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "TargetType", length = 20)
	private TargetType targetType;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "ActionType", length = 20)
	private ActionType actionType;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "ApprovalStatus", length = 20)
	private ApprovalStatus status;
	
	@Column(name = "Reason", columnDefinition = "NVARCHAR(MAX)")
	private String reason;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopID")
	private Shop requestedBy;
	
	@Column(nullable = false)
	private LocalDateTime requestedAt;
	
	private LocalDateTime reviewedAt;

}
