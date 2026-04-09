package com.alotra.service.approval_request.approval;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alotra.entity.ApprovalRequest;
import com.alotra.enums.ApprovalStatus;
import com.alotra.repository.approval_request.ApprovalRequestRepository;

import jakarta.transaction.Transactional;

@Service
public class ApprovalAdminService {

	@Autowired 
	private ApprovalRequestRepository approvalRequestRepository;
    

    @Autowired
    private Map<String, ApprovalState> stateMap;

    @Transactional
    public void processApprove(Integer requestId) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        ApprovalState state = getHandler(request.getStatus());

        state.approve(request);
        
        approvalRequestRepository.save(request);
    }
    
    @Transactional
    public void reject(Integer requestId, String reason) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        ApprovalState state = getHandler(request.getStatus());

        state.reject(request, reason);

        approvalRequestRepository.save(request);
    }

    private ApprovalState getHandler(ApprovalStatus status) {
        return switch (status) {
            case PENDING -> stateMap.get("PENDING_STATE");
            case APPROVED -> stateMap.get("APPROVED_STATE");
            case REJECTED -> stateMap.get("REJECTED_STATE");
        };
    }
}
