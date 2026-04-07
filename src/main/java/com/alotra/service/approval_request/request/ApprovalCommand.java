package com.alotra.service.approval_request.request;


import com.alotra.dto.product.ProductRequestDTO;

public interface ApprovalCommand {
	void execute(ProductRequestDTO request, Integer userId);
}
