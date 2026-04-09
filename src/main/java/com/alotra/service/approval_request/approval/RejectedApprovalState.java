package com.alotra.service.approval_request.approval;

import org.springframework.stereotype.Component;

import com.alotra.entity.ApprovalRequest;

@Component("REJECTED_STATE")
public class RejectedApprovalState implements ApprovalState{

	@Override
    public void approve(ApprovalRequest request) {
        throw new IllegalStateException("Yêu cầu đã bị từ chối, không thể duyệt lại.");
    }

    @Override
    public void reject(ApprovalRequest request, String reason) {
        throw new IllegalStateException("Yêu cầu này đã bị từ chối trước đó.");
    }

	@Override
	public String getStateName() {
		// TODO Auto-generated method stub
		return null;
	}

}
