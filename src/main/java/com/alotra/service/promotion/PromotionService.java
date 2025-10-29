package com.alotra.service.promotion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alotra.entity.product.Product;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.promotion.PromotionProduct;
import com.alotra.entity.promotion.PromotionProductId;
import com.alotra.entity.shop.Shop;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.promotion.PromotionProductRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.repository.shop.ShopRepository;

@Service
public class PromotionService {

	@Autowired
	PromotionRepository promotionRepository;

	@Autowired
	ShopRepository shopRepository;

	@Autowired
	ProductRepository productRepository;
	
	@Autowired
	PromotionProductRepository promotionProductRepository;
	
	public List<Promotion> getAllPromotionsApproval() {
		return promotionRepository.findByStatus(1);
	}

	public List<Promotion> getAllPromotions() {
		return promotionRepository.findAll();
	}

	public Optional<Promotion> findById(Integer Id) {
		return promotionRepository.findById(Id);
	}

	public Promotion save(Promotion promotion) {
		return promotionRepository.save(promotion);
	}

	public Promotion findById1(Integer id) {
		return promotionRepository.findById(id).orElse(null);
	}

	public boolean existsByPromoCode(String promoCode) {
		return promotionRepository.existsByPromoCode(promoCode);
	}

	public Promotion findByPromoCode(String promoCode) {
		return promotionRepository.findByPromoCode(promoCode);
	}

	public void savePromotionProducts(Integer promotionId, List<Integer> productIds) {

	    // Lấy Promotion
	    Promotion promotion = promotionRepository.findById(promotionId)
	                              .orElseThrow(() -> new RuntimeException("Promotion not found"));

	    for (Integer productId : productIds) {
	        Product product = productRepository.findById(productId)
	                              .orElseThrow(() -> new RuntimeException("Product not found"));

	        // Tạo PromotionProduct mới
	        PromotionProduct pp = new PromotionProduct();

	        // Gán EmbeddedId
	        pp.setId(new PromotionProductId(promotionId, productId));

	        // Gán quan hệ
	        pp.setPromotion(promotion);
	        pp.setProduct(product);
	     

	        // Lưu vào database
	        promotionProductRepository.save(pp);
	    }
	}


	
}
