package com.alotra.service.approval_request.approval;

import org.springframework.stereotype.Component;

import com.alotra.entity.ApprovalRequest;

@Component("APPROVED_STATE")
public class ApprovedApprovalState implements ApprovalState{

	@Override
    public void approve(ApprovalRequest request) {
        throw new IllegalStateException("Yêu cầu này đã được duyệt rồi.");
    }

    @Override
    public void reject(ApprovalRequest request, String reason) {
        throw new IllegalStateException("Yêu cầu đã duyệt không thể từ chối.");
    }

	@Override
	public String getStateName() {
		// TODO Auto-generated method stub
		return null;
	}

}
