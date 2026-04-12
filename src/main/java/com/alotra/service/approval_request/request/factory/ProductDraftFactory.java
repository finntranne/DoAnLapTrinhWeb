package com.alotra.service.approval_request.request.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.entity.draft.ProductDraft;
import com.alotra.repository.approval_request.ProductDraftRepository;
import com.alotra.service.approval_request.request.builder.ProductDraftBuilder;

@Component
public class ProductDraftFactory implements DraftAbstractFactory<ProductDraft>{

	@Autowired
	private ProductDraftBuilder builder;
	
	@Autowired
    private ProductDraftRepository productDraftRepository;

    @Override
    public ProductDraft createDraft(Object requestDTO, Integer userId) {
        return builder.build((ProductRequestDTO) requestDTO, userId);
    }

	@Override
	public void save(ProductDraft draft) {
		productDraftRepository.save(draft);
		
	}
}
