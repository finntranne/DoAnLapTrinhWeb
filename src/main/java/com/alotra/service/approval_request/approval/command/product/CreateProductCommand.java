package com.alotra.service.approval_request.approval.command.product;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alotra.entity.ApprovalRequest;
import com.alotra.entity.draft.ProductDraft;
import com.alotra.entity.product.Product;
import com.alotra.repository.approval_request.ProductDraftRepository;
import com.alotra.repository.product.ProductRepository;
import com.alotra.service.approval_request.approval.command.ApprovalCommand;

@Component("PRODUCT_CREATE")
public class CreateProductCommand implements ApprovalCommand{

	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
    private ProductDraftRepository productDraftRepository;
	
	@Autowired
	private ProductMapper mapper;
    
	@Override
	public void execute(ApprovalRequest request) {
		ProductDraft draft = productDraftRepository.findById(request.getTargetId())
                .orElseThrow();

		Product product = mapper.toEntity(draft);
		productRepository.save(product);
	}
	

}
