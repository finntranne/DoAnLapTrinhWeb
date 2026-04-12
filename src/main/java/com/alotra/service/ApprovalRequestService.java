package com.alotra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.alotra.entity.ApprovalRequest;
import com.alotra.enums.TargetType;
import com.alotra.repository.approval_request.ApprovalRequestRepository;

public class ApprovalRequestService {

	@Autowired
	private ApprovalRequestRepository approvalRequestRepository;
	
	public Page<ApprovalRequest> findByTarget(TargetType type, Pageable pageable) {
	    return approvalRequestRepository.findByTargetType(type, pageable);
	}
}
