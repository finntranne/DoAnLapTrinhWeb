package com.alotra.repository.promotion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Product;
import com.alotra.entity.promotion.PromotionProduct;
import com.alotra.entity.promotion.PromotionProductId;

@Repository
public interface PromotionProductRepository extends JpaRepository<PromotionProduct, PromotionProductId> {
    
    List<PromotionProduct> findByPromotion_PromotionId(Integer promotionId);
    
    List<PromotionProduct> findByProduct_ProductID(Integer productId);
    
    void deleteByPromotion_PromotionId(Integer promotionId);
    
    @Query("SELECT pp.product FROM PromotionProduct pp " +
           "WHERE pp.promotion.promotionId = :promotionId")
    List<Product> findProductsByPromotionId(@Param("promotionId") Integer promotionId);
}