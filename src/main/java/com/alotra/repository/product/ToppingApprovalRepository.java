package com.alotra.repository.product;

import com.alotra.entity.product.ProductApproval;
import com.alotra.entity.product.ToppingApproval;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToppingApprovalRepository extends JpaRepository<ToppingApproval, Integer> {

    List<ToppingApproval> findByTopping_ToppingIDAndStatus(Integer toppingId, String status);
    
    Optional<ToppingApproval> findTopByTopping_ToppingIDOrderByRequestedAtDesc(Integer toppingId);

    List<ToppingApproval> findByShop_ShopIdAndStatusOrderByRequestedAtDesc(Integer shopId, String status);

    @Query("SELECT COUNT(ta) FROM ToppingApproval ta WHERE ta.shop.shopId = :shopId AND ta.status = 'Pending'")
    Long countPendingByShopId(@Param("shopId") Integer shopId);
    
    Page<ToppingApproval> findByStatus(String status, Pageable pageable);
    
    @Transactional
	@Modifying
	@Query(value = "EXEC sp_ApproveToppingChange @ApprovalID = :approvalId, @ReviewedByUserID = :reviewedByUserId", nativeQuery = true)
	void approveToppingChange(@Param("approvalId") Integer approvalId, @Param("reviewedByUserId") Integer reviewedByUserId);
    
    @Transactional
    @Modifying
    @Query(value = "EXEC sp_RejectToppingChange @ApprovalID = :approvalId, @ReviewedByUserID = :reviewedByUserId, @RejectionReason = :reason", nativeQuery = true)
    void rejectToppingChange(@Param("approvalId") Integer approvalId,
                             @Param("reviewedByUserId") Integer reviewedByUserId,
                             @Param("reason") String rejectionReason);
}