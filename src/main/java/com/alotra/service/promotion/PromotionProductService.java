package com.alotra.service.promotion;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alotra.entity.promotion.PromotionProduct;
import com.alotra.repository.promotion.PromotionProductRepository;

@Service
public class PromotionProductService {
	
	@Autowired
	PromotionProductRepository promotionProductRepository;
	
	public List<PromotionProduct> findByPromotionId(Integer promotionId){
		return promotionProductRepository.findByPromotion_PromotionId(promotionId);
	}
	
	

}
