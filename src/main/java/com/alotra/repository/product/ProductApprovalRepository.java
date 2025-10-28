package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.entity.product.ProductApproval;

@Repository
public interface ProductApprovalRepository extends JpaRepository<ProductApproval, Integer> {

	List<ProductApproval> findByProduct_Shop_ShopIdAndStatusOrderByRequestedAtDesc(Integer shopId, String status);

	List<ProductApproval> findByStatusOrderByRequestedAtDesc(String status);

	Optional<ProductApproval> findByProduct_ProductIDAndStatusAndActionType(Integer productID, String status,
			String actionType);

	@Query("SELECT COUNT(pa) FROM ProductApproval pa "
			+ "WHERE pa.product.shop.shopId = :shopId AND pa.status = 'Pending'")
	Long countPendingByShopId(@Param("shopId") Integer shopId);

	@Query("SELECT pa FROM ProductApproval pa " + "WHERE pa.status = 'Pending' " + "ORDER BY pa.requestedAt ASC")
	List<ProductApproval> findAllPendingApprovals();

	@Query("SELECT COUNT(pa) FROM ProductApproval pa WHERE pa.status = 'Pending'")
	Long countAllPending();

	Optional<ProductApproval> findTopByProduct_ProductIDAndStatusOrderByRequestedAtDesc(Integer productId,
			String status);
	
	// Tìm approval theo productId và status (không cần actionType)
    List<ProductApproval> findByProduct_ProductIDAndStatus(Integer productId, String status);
    
    Optional<ProductApproval> findTopByProduct_ProductIDOrderByRequestedAtDesc(Integer productId);

	Page<ProductApproval> findByStatus(String status, Pageable pageable);

	@Transactional
	@Modifying
	@Query(value = "EXEC sp_ApproveProductChange @ApprovalID = :approvalId, @ReviewedByUserID = :reviewedByUserId", nativeQuery = true)
	void approveProductChange(@Param("approvalId") Integer approvalId, @Param("reviewedByUserId") Integer reviewedByUserId);

	@Transactional
    @Modifying
    @Query(value = "EXEC sp_RejectProductChange @ApprovalID = :approvalId, @ReviewedByUserID = :reviewedByUserId, @RejectionReason = :reason", nativeQuery = true)
    void rejectProductChange(@Param("approvalId") Integer approvalId,
                             @Param("reviewedByUserId") Integer reviewedByUserId,
                             @Param("reason") String rejectionReason);
}
