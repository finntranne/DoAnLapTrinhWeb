package com.alotra.service.approval_request.request.builder;


import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alotra.dto.topping.ToppingRequestDTO;
import com.alotra.entity.draft.ToppingDraft;
import com.alotra.entity.product.Topping;
import com.alotra.entity.shop.Shop;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.service.cloudinary.CloudinaryService;

@Component
public class ToppingDraftBuilder implements DraftBuilder<ToppingDraft, ToppingRequestDTO>{

	@Autowired
	private ShopRepository shopRepository;
	
	@Autowired
	private ToppingRepository toppingRepository;
	
	@Autowired
	private CloudinaryService cloudinaryService;
	
	@Override
	public ToppingDraft build(ToppingRequestDTO request, Integer userId) {
		// Validate shopId
		if (request.getShopId() == null) {
			throw new IllegalArgumentException("ShopId không được phép NULL. Vui lòng đăng ký shop trước.");
		}
		
		Shop shop = shopRepository.findById(request.getShopId())
	              .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop với ID: " + request.getShopId()));
		
		Topping topping = null;
	    if (request.getToppingId() != null) {
	        topping = toppingRepository.findById(request.getToppingId()).orElse(null);
	    }
	    
		ToppingDraft draft = new ToppingDraft();
		draft.setShop(shop);
		draft.setToppingName(request.getToppingName());
		draft.setPrice(request.getAdditionalPrice());
		
		String finalImageUrl = handleToppingImage(request, userId);
	    draft.setImageURL(finalImageUrl);
    
		return draft;
	}
	
	private String handleToppingImage(ToppingRequestDTO request, Integer userId) {
	    
	    if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
	        try {
	            Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(
	                request.getImageFile(), "toppings", userId);
	            return uploadResult.get("secure_url");
	        } catch (Exception ex) {
	            throw new RuntimeException("Lỗi upload ảnh topping mới", ex);
	        }
	    }
	    return request.getExistingImageUrl();
	}

}
