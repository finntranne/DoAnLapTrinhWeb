package com.alotra.repository.promotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.promotion.Promotion;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    
    List<Promotion> findByCreatedByShopID_ShopIdOrderByCreatedAtDesc(Integer shopId);
    
    @Query("SELECT p FROM Promotion p " +
           "WHERE p.createdByShopID.shopId = :shopId " +
           "AND (:status IS NULL OR p.status = :status) " +
           "ORDER BY p.createdAt DESC")
    Page<Promotion> findShopPromotionsByStatus(
        @Param("shopId") Integer shopId,
        @Param("status") Byte status,
        Pageable pageable
    );
    
    Optional<Promotion> findByPromoCodeAndStatus(String promoCode, Byte status);
    
    @Query("SELECT p FROM Promotion p " +
           "WHERE p.status = 1 " +
           "AND p.startDate <= :now " +
           "AND p.endDate >= :now " +
           "AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);
    
    @Query("SELECT p FROM Promotion p " +
           "WHERE p.createdByShopID.shopId = :shopId " +
           "AND p.status = 1 " +
           "AND p.startDate <= :now " +
           "AND p.endDate >= :now")
    List<Promotion> findActivePromotionsByShop(
        @Param("shopId") Integer shopId, 
        @Param("now") LocalDateTime now
    );
    
    @Query("SELECT p FROM Promotion p WHERE p.createdByShopID.shopId = :shopId " +
            "AND (:status IS NULL OR " + // Nếu status là null, bỏ qua điều kiện lọc status
            "(:status = 1 AND p.status = 1 AND p.endDate >= :now) OR " + // status=1: Đang hoạt động (Status=1 VÀ Chưa hết hạn)
            "(:status = 0 AND p.status = 0) OR " +                      // status=0: Chưa kích hoạt (Status=0)
            "(:status = 2 AND p.status = 1 AND p.endDate < :now))")    // status=2: Đã kết thúc (Status=1 VÀ Đã hết hạn)
     Page<Promotion> findShopPromotionsFiltered(
             @Param("shopId") Integer shopId,
             @Param("status") Byte status, // Giá trị status từ controller (null, 0, 1, 2)
             @Param("now") LocalDateTime now, // Thời gian hiện tại để so sánh EndDate
             Pageable pageable);
    
    @Query("SELECT p FROM Promotion p " +
            "WHERE p.createdByShopID.shopId = :shopId " +
            "AND p.promotionType = 'PRODUCT' " +
            "AND p.status = 1 " + // Chỉ lấy các KM đã được duyệt và đang hoạt động
            "AND p.endDate > CURRENT_TIMESTAMP") // Chỉ lấy KM còn hạn
     List<Promotion> findAllActiveProductPromotionsByShop(@Param("shopId") Integer shopId);
}
