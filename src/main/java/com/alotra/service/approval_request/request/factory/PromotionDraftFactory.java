package com.alotra.service.approval_request.request.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alotra.dto.promotion.PromotionRequestDTO;
import com.alotra.entity.draft.PromotionDraft;
import com.alotra.repository.approval_request.PromotionDraftRepository;
import com.alotra.service.approval_request.request.builder.PromotionDraftBuilder;

@Component
public class PromotionDraftFactory implements DraftAbstractFactory<PromotionDraft> {
	
	@Autowired
	private PromotionDraftBuilder builder;
	
	@Autowired
    private PromotionDraftRepository promotionDraftRepository;

	@Override
	public PromotionDraft createDraft(Object requestDTO, Integer userId) {
		return builder.build((PromotionRequestDTO) requestDTO, userId);
	}

	@Override
	public void save(PromotionDraft draft) {
		promotionDraftRepository.save(draft);
	}

}
