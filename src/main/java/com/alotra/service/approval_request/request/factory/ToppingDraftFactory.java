package com.alotra.service.approval_request.request.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alotra.dto.topping.ToppingRequestDTO;
import com.alotra.entity.draft.ToppingDraft;
import com.alotra.repository.approval_request.ToppingDraftRepository;
import com.alotra.service.approval_request.request.builder.ToppingDraftBuilder;

@Component
public class ToppingDraftFactory implements DraftAbstractFactory<ToppingDraft>{

	@Autowired
	private ToppingDraftBuilder builder;
	
	@Autowired
    private ToppingDraftRepository toppingDraftRepository;
	
	@Override
	public ToppingDraft createDraft(Object requestDTO, Integer userId) {
		return builder.build((ToppingRequestDTO) requestDTO, userId);
	}

	@Override
	public void save(ToppingDraft draft) {
		toppingDraftRepository.save(draft);
		
	}

}
