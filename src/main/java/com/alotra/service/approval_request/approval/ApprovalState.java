package com.alotra.service.approval_request.approval;

import com.alotra.entity.ApprovalRequest;

public interface ApprovalState {

	void approve(ApprovalRequest request);
    void reject(ApprovalRequest request, String reason);
    
    String getStateName();
}
