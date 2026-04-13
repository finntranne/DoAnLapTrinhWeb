package com.alotra.service.approval_request.approval.command.topping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alotra.entity.ApprovalRequest;
import com.alotra.entity.draft.ToppingDraft;
import com.alotra.entity.product.Topping;
import com.alotra.repository.approval_request.ToppingDraftRepository;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.service.approval_request.approval.command.ApprovalCommand;

@Component("TOPPING_UPDATE")
public class UpdateToppinngCommand implements ApprovalCommand{

	@Autowired
	private ToppingRepository toppingRepository;
	
	@Autowired
    private ToppingDraftRepository toppingDraftRepository;
	
	@Autowired
	private ToppingMapper mapper;
    
	@Override
	public void execute(ApprovalRequest request) {
		ToppingDraft draft = toppingDraftRepository.findById(request.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ToppingDraft với ID: " + request.getTargetId()));

		Topping topping = draft.getTopping();
		if (topping == null) {
			throw new IllegalArgumentException("Không tìm thấy Topping gốc để cập nhật. Draft ToppingID: " + draft.getId());
		}
		
		mapper.updateEntity(topping, draft);
		
		toppingRepository.save(topping);
	}


}
