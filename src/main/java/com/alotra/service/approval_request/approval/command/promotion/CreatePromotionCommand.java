package com.alotra.service.approval_request.approval.command.promotion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alotra.entity.ApprovalRequest;
import com.alotra.entity.draft.PromotionDraft;
import com.alotra.entity.promotion.Promotion;
import com.alotra.repository.approval_request.PromotionDraftRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.service.approval_request.approval.command.ApprovalCommand;

@Component("PROMOTION_CREATE")
public class CreatePromotionCommand implements ApprovalCommand{
	
	@Autowired
	private PromotionRepository promotionRepository;
	
	@Autowired
	private PromotionDraftRepository promotionDraftRepository;
	
	@Autowired
	private PromotionMapper mapper;

	@Override
	public void execute(ApprovalRequest request) {
		PromotionDraft draft = promotionDraftRepository.findById(request.getTargetId())
                .orElseThrow();

		Promotion promotion = mapper.toEntity(draft);
		promotionRepository.save(promotion);
		
	}

}
