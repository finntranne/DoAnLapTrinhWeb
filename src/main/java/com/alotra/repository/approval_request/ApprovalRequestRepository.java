package com.alotra.repository.approval_request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.ApprovalRequest;
import com.alotra.enums.ApprovalStatus;
import com.alotra.enums.TargetType;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Integer>{

	Page<ApprovalRequest> findByStatus(ApprovalStatus status, Pageable pageable);
	
	Page<ApprovalRequest> findByTargetType(TargetType targetType, Pageable pageable);
}
