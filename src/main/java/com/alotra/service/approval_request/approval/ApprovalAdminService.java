package com.alotra.service.approval_request.approval;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alotra.entity.ApprovalRequest;
import com.alotra.enums.ApprovalStatus;
import com.alotra.repository.approval_request.ApprovalRequestRepository;
import com.alotra.service.approval_request.approval.command.ApprovalCommand;

import jakarta.transaction.Transactional;

@Service
public class ApprovalAdminService {
	
	
	@Autowired 
	private ApprovalRequestRepository approvalRequestRepository;
	
	@Autowired
	private Map<String, ApprovalCommand> commandMap;

	@Transactional
    public void approve(ApprovalRequest request) {

        String key = request.getTargetType().name() + "_" + request.getActionType().name();

        ApprovalCommand command = commandMap.get(key);
        
        if (command == null) {
            throw new IllegalArgumentException("No command found for key: " + key);
        }

        command.execute(request);
        
        request.setStatus(ApprovalStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        
        approvalRequestRepository.save(request);
    }

	@Transactional
	public void reject(ApprovalRequest request, String reason) {

	    request.setStatus(ApprovalStatus.REJECTED);
	    request.setReviewedAt(LocalDateTime.now());
	    request.setReason(reason);

	    approvalRequestRepository.save(request);
	}
	
//    
//
//    @Autowired
//    private Map<String, ApprovalState> stateMap;
//
//    @Transactional
//    public void processApprove(Integer requestId) {
//        ApprovalRequest request = approvalRequestRepository.findById(requestId)
//                .orElseThrow(() -> new RuntimeException("Request not found"));
//
//        ApprovalState state = getHandler(request.getStatus());
//
//        state.approve(request);
//        
//        approvalRequestRepository.save(request);
//    }
//    
//    @Transactional
//    public void reject(Integer requestId, String reason) {
//        ApprovalRequest request = approvalRequestRepository.findById(requestId)
//                .orElseThrow(() -> new RuntimeException("Request not found"));
//
//        ApprovalState state = getHandler(request.getStatus());
//
//        state.reject(request, reason);
//
//        approvalRequestRepository.save(request);
//    }
//
//    private ApprovalState getHandler(ApprovalStatus status) {
//        return switch (status) {
//            case PENDING -> stateMap.get("PENDING_STATE");
//            case APPROVED -> stateMap.get("APPROVED_STATE");
//            case REJECTED -> stateMap.get("REJECTED_STATE");
//        };
//    }
}
