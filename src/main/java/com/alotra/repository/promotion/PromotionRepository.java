package com.alotra.repository.promotion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.shop.Shop;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    /**
     * Tìm khuyến mãi bằng mã code (ví dụ: "FREESHIP", "GIAM10K").
     * Dùng Optional vì mã code có thể không tồn tại.
     *
     * @param promoCode Mã khuyến mãi.
     * @return Optional<Promotion>
     */
    Optional<Promotion> findByPromoCode(String promoCode);

    /**
     * Tìm tất cả khuyến mãi được tạo bởi một Shop cụ thể.
     *
     * @param shop Đối tượng Shop (người tạo).
     * @return Danh sách Promotion.
     */
    List<Promotion> findByShop(Shop shop);

    /**
     * Tìm tất cả khuyến mãi do Admin tạo (Shop là NULL).
     */
    List<Promotion> findByShopIsNull();

    /**
     * Tìm tất cả khuyến mãi theo trạng thái (ví dụ: 1 = Active, 0 = Inactive).
     *
     * @param status Trạng thái.
     * @return Danh sách Promotion.
     */
    List<Promotion> findByStatus(int status);

    /**
     * Tìm tất cả các khuyến mãi ĐANG CÓ HIỆU LỰC.
     * (Tức là: Status = 1, ngày hiện tại nằm giữa StartDate và EndDate)
     *
     * @param currentDate Ngày hiện tại.
     * @param activeStatus Mã trạng thái active (ví dụ: 1).
     * @return Danh sách các Promotion đang chạy.
     */
    @Query("SELECT p FROM Promotion p WHERE p.status = :status " +
           "AND p.startDate <= :currentDate " +
           "AND p.endDate >= :currentDate")
    List<Promotion> findActivePromotions(
        @Param("currentDate") LocalDate currentDate,
        @Param("status") int activeStatus
    );

    /**
     * Tìm các khuyến mãi áp dụng cho một sản phẩm cụ thể.
     * (Sử dụng join qua bảng Promotion_Product_Applicability)
     *
     * @param productId ID của sản phẩm.
     * @return Danh sách Promotion.
     */
    @Query("SELECT p FROM Promotion p JOIN p.products prod WHERE prod.id = :productId")
    List<Promotion> findPromotionsByApplicableProduct(@Param("productId") int productId);

    /**
     * Tìm các khuyến mãi áp dụng cho một shop cụ thể.
     * (Sử dụng join qua bảng Promotion_Shop_Applicability)
     *
     * @param shopId ID của shop.
     * @return Danh sách Promotion.
     */
    @Query("SELECT p FROM Promotion p JOIN p.shops s WHERE s.id = :shopId")
    List<Promotion> findPromotionsByApplicableShop(@Param("shopId") int shopId);
}