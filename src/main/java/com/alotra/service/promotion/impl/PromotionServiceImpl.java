package com.alotra.service.promotion.impl;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alotra.entity.product.Product;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.shop.Shop;
import com.alotra.execption.InvalidPromotionException;
import com.alotra.execption.ResourceNotFoundException;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.service.promotion.PromotionService;

@Service
public class PromotionServiceImpl implements PromotionService{
	
	@Autowired
	PromotionRepository promotionRepository;
	
	@Autowired
    ShopRepository shopRepository;
	
	@Autowired
    ProductRepository productRepository;

	@Override
	public Promotion createPromotion(Promotion promotion, Integer creatorShopId, Set<Integer> applicableShopIds,
			Set<Integer> applicableProductIds) {
		// 1. Validate dữ liệu cơ bản
        validatePromotionData(promotion);

        // 2. Xử lý logic nghiệp vụ cho Người tạo (Creator) và Phạm vi (Applicability)
        if (creatorShopId != null) {
            // ----- TRƯỜNG HỢP VENDOR TẠO -----
            Shop creatorShop = shopRepository.findById(creatorShopId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator shop not found"));
            
            promotion.setShop(creatorShop); // Đặt người tạo là Shop

            // Quy tắc: Vendor không thể tạo KM cho shop khác
            if (applicableShopIds != null && !applicableShopIds.isEmpty()) {
                throw new InvalidPromotionException("Vendor cannot create promotions for other shops.");
            }

            if (applicableProductIds == null || applicableProductIds.isEmpty()) {
                // KM cho toàn bộ shop của Vendor -> Thêm chính shop đó vào danh sách áp dụng
                Set<Shop> shopSet = new HashSet<>();
                shopSet.add(creatorShop);
                promotion.setShops(shopSet);
            } else {
                // KM cho sản phẩm cụ thể -> Validate sản phẩm phải thuộc shop
                Set<Product> products = new HashSet<>(productRepository.findAllById(applicableProductIds));
                for (Product p : products) {
                    if (p.getShop().getShopId() != creatorShopId) {
                        throw new InvalidPromotionException("Cannot apply promotion to product from another shop.");
                    }
                }
                promotion.setProducts(products);
            }

        } else {
            // ----- TRƯỜNG HỢP ADMIN TẠO -----
            promotion.setShop(null); // Admin tạo

            // Admin có thể gán cho nhiều shop
            if (applicableShopIds != null && !applicableShopIds.isEmpty()) {
                Set<Shop> shops = new HashSet<>(shopRepository.findAllById(applicableShopIds));
                promotion.setShops(shops);
            }

            // Admin có thể gán cho nhiều sản phẩm (bất kể shop nào)
            if (applicableProductIds != null && !applicableProductIds.isEmpty()) {
                Set<Product> products = new HashSet<>(productRepository.findAllById(applicableProductIds));
                promotion.setProducts(products);
            }
            
            // Nếu cả 2 set (shops, products) đều rỗng -> KM này áp dụng TOÀN SÀN
        }

        // 3. Đặt trạng thái mặc định và lưu
        promotion.setStatus(1); // Mặc định là Active
        return promotionRepository.save(promotion);
	}

	@Override
	@Transactional
	public Promotion updatePromotion(int id, Promotion promotionDetails) {
		
		Promotion existingPromo = getPromotionById(id);
		
		// Chỉ cho phép cập nhật một số trường, không cho đổi người tạo
        existingPromo.setPromoCode(promotionDetails.getPromoCode());
        existingPromo.setDiscountType(promotionDetails.getDiscountType());
        existingPromo.setDiscountValue(promotionDetails.getDiscountValue());
        existingPromo.setStartDate(promotionDetails.getStartDate());
        existingPromo.setEndDate(promotionDetails.getEndDate());
        existingPromo.setMinOrderValue(promotionDetails.getMinOrderValue());
        
        validatePromotionData(existingPromo); // Kiểm tra lại logic

        return promotionRepository.save(existingPromo);
	}

	@Override
	public Promotion getPromotionById(int id) {
		return promotionRepository.findById(id)
	            .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with id: " + id));
	}

	@Override
	public Promotion getPromotionByCode(String code) {
		return promotionRepository.findByPromoCode(code)
	            .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with code: " + code));
	}

	@Override
	public List<Promotion> getActivePromotions() {
		return promotionRepository.findActivePromotions(LocalDate.now(), 1);
	}

	@Override
	@Transactional
	public void deactivatePromotion(int id) {
		Promotion promo = getPromotionById(id);
        promo.setStatus(0); // Giả sử 0 = Inactive
        promotionRepository.save(promo);
	}

	@Override
	public List<Promotion> getPromotionsByCreatorShop(int shopId) {
		Shop shop = shopRepository.findById(shopId)
	            .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
	        return promotionRepository.findByShop(shop);
	}
	
	@Override
	public List<Promotion> getAllPromotions() {
		return promotionRepository.findAll();
	}
	
	private void validatePromotionData(Promotion promotion) {

	    // 1. Kiểm tra các trường bắt buộc (Not Null / Not Empty)
	    if (!StringUtils.hasText(promotion.getPromoCode())) {
	        throw new InvalidPromotionException("PromoCode is required.");
	    }
	    if (!StringUtils.hasText(promotion.getDiscountType())) {
	        throw new InvalidPromotionException("DiscountType is required.");
	    }
	    if (promotion.getDiscountValue() == null) {
	        throw new InvalidPromotionException("DiscountValue is required.");
	    }
	    if (promotion.getStartDate() == null) {
	        throw new InvalidPromotionException("StartDate is required.");
	    }
	    if (promotion.getEndDate() == null) {
	        throw new InvalidPromotionException("EndDate is required.");
	    }

	    // 2. Kiểm tra logic ngày tháng
	    // Ngày kết thúc không được trước ngày bắt đầu
	    if (promotion.getEndDate().isBefore(promotion.getStartDate())) {
	        throw new InvalidPromotionException("End date must be on or after start date.");
	    }
	    // (Tùy chọn) Bạn có thể muốn cấm tạo KM trong quá khứ
	    if (promotion.getEndDate().isBefore(LocalDate.now())) {
	         throw new InvalidPromotionException("End date cannot be in the past.");
	    }

	    // 3. Kiểm tra logic loại và giá trị giảm giá
	    String discountType = promotion.getDiscountType();
	    double discountValue = promotion.getDiscountValue();

	    // Định nghĩa các loại hợp lệ (bạn nên dùng Enum cho việc này)
	    final Set<String> VALID_TYPES = Set.of("Percentage", "FixedAmount", "FreeShip");

	    if (!VALID_TYPES.contains(discountType)) {
	        throw new InvalidPromotionException("Invalid DiscountType: " + discountType);
	    }

	    // Nếu là phần trăm, giá trị phải từ 1-100
	    if ("Percentage".equals(discountType)) {
	        if (discountValue <= 0 || discountValue > 100) {
	            throw new InvalidPromotionException("Percentage discount value must be between 1 and 100.");
	        }
	    } 
	    // Nếu là tiền cố định hoặc freeship (nếu freeship cũng có giá trị), phải lớn hơn 0
	    else if ("FixedAmount".equals(discountType) || "FreeShip".equals(discountType)) {
	         if (discountValue <= 0) {
	            throw new InvalidPromotionException("Discount value must be greater than 0.");
	        }
	    }

	    // 4. Kiểm tra giá trị đơn hàng tối thiểu
	    if (promotion.getMinOrderValue() != null && promotion.getMinOrderValue() < 0) {
	        throw new InvalidPromotionException("Minimum order value cannot be negative.");
	    }

	    // 5. Kiểm tra trùng lặp PromoCode (đã có trong code trước)
	    // Rất quan trọng: Phải kiểm tra xem code đã tồn tại cho một ID khác chưa
	    promotionRepository.findByPromoCode(promotion.getPromoCode())
	        .ifPresent(existingPromo -> {
	            // Nếu tìm thấy một promo có cùng code, VÀ nó có ID khác với promo đang check
	            // (tránh lỗi khi đang update chính nó)
	            if (existingPromo.getPromotiontId() != promotion.getPromotiontId()) {
	                throw new InvalidPromotionException("PromoCode '" + promotion.getPromoCode() + "' already exists.");
	            }
	        });
	}

	

}
