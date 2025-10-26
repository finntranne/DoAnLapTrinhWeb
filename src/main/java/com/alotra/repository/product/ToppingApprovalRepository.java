package com.alotra.repository.product;

import com.alotra.entity.product.ToppingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToppingApprovalRepository extends JpaRepository<ToppingApproval, Integer> {

    List<ToppingApproval> findByTopping_ToppingIDAndStatus(Integer toppingId, String status);
    
    Optional<ToppingApproval> findTopByTopping_ToppingIDOrderByRequestedAtDesc(Integer toppingId);

    List<ToppingApproval> findByShop_ShopIdAndStatusOrderByRequestedAtDesc(Integer shopId, String status);

    @Query("SELECT COUNT(ta) FROM ToppingApproval ta WHERE ta.shop.shopId = :shopId AND ta.status = 'Pending'")
    Long countPendingByShopId(@Param("shopId") Integer shopId);
}