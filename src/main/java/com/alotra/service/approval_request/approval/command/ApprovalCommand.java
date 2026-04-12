package com.alotra.service.approval_request.approval.command;

import com.alotra.entity.ApprovalRequest;

public interface ApprovalCommand {
	
	void execute(ApprovalRequest request);
}
