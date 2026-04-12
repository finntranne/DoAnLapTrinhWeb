package com.alotra.service.approval_request.approval.command.promotion;

import org.springframework.stereotype.Component;

import com.alotra.entity.draft.PromotionDraft;
import com.alotra.entity.promotion.Promotion;

@Component
public class PromotionMapper {

	public Promotion toEntity(PromotionDraft draft) {
		Promotion promotion = new Promotion();
        mapDraftToPromotion(draft, promotion);
        return promotion;
    }

    public void updateEntity(Promotion promotion, PromotionDraft draft) {
        mapDraftToPromotion(draft, promotion);
    }
    
    private void mapDraftToPromotion(PromotionDraft draft, Promotion promotion) {
    	promotion.setCreatedByShopID(draft.getCreatedByShopID());
    	promotion.setDescription(draft.getDescription());
    	promotion.setDiscountType(draft.getDiscountType());
    	promotion.setDiscountValue(draft.getDiscountValue());
    	promotion.setMaxDiscountAmount(draft.getMaxDiscountAmount());
    	promotion.setMinOrderValue(draft.getMinOrderValue());
    	promotion.setPromoCode(draft.getPromoCode());
    	promotion.setPromotionName(draft.getPromotionName());
    	promotion.setPromotionType(draft.getPromotionType());
    	promotion.setStartDate(draft.getStartDate());
    	promotion.setEndDate(draft.getEndDate());
    	promotion.setUsageLimit(draft.getUsageLimit());
    	promotion.setStatus((byte) 1);

    }
}
