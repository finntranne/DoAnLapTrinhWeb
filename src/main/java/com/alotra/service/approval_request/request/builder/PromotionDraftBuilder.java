package com.alotra.service.approval_request.request.builder;

import java.time.LocalDateTime;

import org.apache.poi.ss.formula.functions.Now;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alotra.dto.promotion.PromotionRequestDTO;
import com.alotra.entity.draft.PromotionDraft;
import com.alotra.entity.draft.ToppingDraft;
import com.alotra.entity.product.Topping;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.shop.Shop;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.repository.shop.ShopRepository;

@Component
public class PromotionDraftBuilder implements DraftBuilder<PromotionDraft, PromotionRequestDTO>{

	@Autowired
	private ShopRepository shopRepository;
	
	@Autowired
	private PromotionRepository promotionRepository;
	
	@Override
	public PromotionDraft build(PromotionRequestDTO request, Integer userId) {
		Shop shop = shopRepository.findById(request.getShopId())
	              .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop"));
		
		Promotion promotion = null;
	    if (request.getPromotionId() != null) {
	        promotion = promotionRepository.findById(request.getPromotionId()).orElse(null);
	    }
			
		PromotionDraft draft = new PromotionDraft();
		draft.setCreatedByShopID(shop);
		draft.setDescription(request.getDescription());
		draft.setDiscountType(request.getDiscountType());
		draft.setDiscountValue(request.getDiscountValue());
		draft.setStartDate(request.getStartDate());
		draft.setEndDate(request.getEndDate());
		draft.setPromotionName(request.getPromotionName());
		draft.setPromoCode(request.getPromoCode());
		draft.setPromotionType(request.getPromotionType());
		draft.setMaxDiscountAmount(request.getMaxDiscountAmount());
		draft.setMinOrderValue(request.getMinOrderValue());
		draft.setUsageLimit(request.getUsageLimit());
		draft.setStatus((byte) 1);
		draft.setPromotion(promotion);
  
		return draft;
	}

	

}
