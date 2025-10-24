package com.alotra.service.promotion;

import java.util.List;
import java.util.Set;

import com.alotra.entity.promotion.Promotion;

public interface PromotionService {
	
	/**
     * Tạo một khuyến mãi mới với các quy tắc nghiệp vụ phức tạp.
     *
     * @param promotion           Đối tượng Promotion chứa thông tin cơ bản (tên, code, giá trị...).
     * @param creatorShopId       ID của Shop tạo (NULL nếu Admin tạo).
     * @param applicableShopIds   Danh sách ID các Shop được áp dụng (cho Admin).
     * @param applicableProductIds Danh sách ID các Sản phẩm được áp dụng.
     * @return Promotion đã được lưu.
     */
    Promotion createPromotion(Promotion promotion, 
                              Integer creatorShopId, 
                              Set<Integer> applicableShopIds, 
                              Set<Integer> applicableProductIds);

    /**
     * Cập nhật thông tin khuyến mãi.
     *
     * @param id Mã khuyến mãi.
     * @param promotionDetails Thông tin chi tiết để cập nhật.
     * @return Promotion đã cập nhật.
     */
    Promotion updatePromotion(int id, Promotion promotionDetails);

    /**
     * Lấy khuyến mãi bằng ID.
     *
     * @param id Mã khuyến mãi.
     * @return Promotion.
     */
    Promotion getPromotionById(int id);

    /**
     * Lấy khuyến mãi bằng PromoCode.
     *
     * @param code Mã code.
     * @return Promotion.
     */
    Promotion getPromotionByCode(String code);

    /**
     * Lấy tất cả khuyến mãi đang hoạt động (còn hạn và status = 1).
     *
     * @return Danh sách Promotion.
     */
    List<Promotion> getActivePromotions();

    /**
     * Hủy kích hoạt một khuyến mãi (an toàn hơn là xóa).
     *
     * @param id Mã khuyến mãi.
     */
    void deactivatePromotion(int id);

    /**
     * Lấy tất cả khuyến mãi do một Shop cụ thể tạo.
     *
     * @param shopId ID của Shop.
     * @return Danh sách Promotion.
     */
    List<Promotion> getPromotionsByCreatorShop(int shopId);
    
    
    List<Promotion> getAllPromotions();

}
