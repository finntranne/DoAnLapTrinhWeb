package com.alotra.repository.promotion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.promotion.PromotionApproval;

@Repository
public interface PromotionApprovalRepository extends JpaRepository<PromotionApproval, Integer> {
    
    List<PromotionApproval> findByPromotion_CreatedByShopID_ShopIdAndStatusOrderByRequestedAtDesc(
        Integer shopId, String status);
    
    List<PromotionApproval> findByStatusOrderByRequestedAtDesc(String status);
    
    Optional<PromotionApproval> findByPromotion_PromotionIdAndStatusAndActionType(
        Integer promotionId, String status, String actionType);
    
    @Query("SELECT COUNT(pa) FROM PromotionApproval pa " +
           "WHERE pa.promotion.createdByShopID.shopId = :shopId AND pa.status = 'Pending'")
    Long countPendingByShopId(@Param("shopId") Integer shopId);
    
    @Query("SELECT pa FROM PromotionApproval pa " +
           "WHERE pa.status = 'Pending' " +
           "ORDER BY pa.requestedAt ASC")
    List<PromotionApproval> findAllPendingApprovals();
    
    @Query("SELECT COUNT(pa) FROM PromotionApproval pa WHERE pa.status = 'Pending'")
    Long countAllPending();
    
    List<PromotionApproval> findByPromotion_PromotionIdAndStatus(Integer promotionId, String status);
    
    Optional<PromotionApproval> findTopByPromotion_PromotionIdOrderByRequestedAtDesc(Integer promotionId);
}