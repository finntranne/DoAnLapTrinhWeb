package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.ProductApproval;

@Repository
public interface ProductApprovalRepository extends JpaRepository<ProductApproval, Integer> {
    
    List<ProductApproval> findByProduct_Shop_ShopIdAndStatusOrderByRequestedAtDesc(
        Integer shopId, String status);
    
    List<ProductApproval> findByStatusOrderByRequestedAtDesc(String status);
    
    Optional<ProductApproval> findByProduct_ProductIDAndStatusAndActionType(Integer productID, String status, String actionType);

    
    @Query("SELECT COUNT(pa) FROM ProductApproval pa " +
           "WHERE pa.product.shop.shopId = :shopId AND pa.status = 'Pending'")
    Long countPendingByShopId(@Param("shopId") Integer shopId);
    
    @Query("SELECT pa FROM ProductApproval pa " +
           "WHERE pa.status = 'Pending' " +
           "ORDER BY pa.requestedAt ASC")
    List<ProductApproval> findAllPendingApprovals();
    
    @Query("SELECT COUNT(pa) FROM ProductApproval pa WHERE pa.status = 'Pending'")
    Long countAllPending();
}
