package com.alotra.repository.product;

import com.alotra.model.ProductSaleDTO;
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
// import com.alotra.entity.product.ProductApproval; // Không cần import vì không dùng trực tiếp ProductApproval

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

	Page<Product> findByShop_ShopIdOrderByCreatedAtDesc(Integer shopId, Pageable pageable);

	@Query("SELECT COUNT(p) FROM Product p WHERE p.shop.shopId = :shopId AND (:status IS NULL OR p.status = :status)")
	Long countByShopIdAndStatus(@Param("shopId") Integer shopId, @Param("status") Byte status);

	// Giữ nguyên các truy vấn quản lý Shop (searchShopProducts) - NATIVE QUERY
	@Query(value = """
			WITH LatestApproval AS (
			    SELECT
			        pa.ProductID,
			        pa.Status AS LatestApprovalStatus,
			        pa.ActionType AS LatestActionType,
			        ROW_NUMBER() OVER(PARTITION BY pa.ProductID ORDER BY pa.RequestedAt DESC) as rn
			    FROM dbo.ProductApprovals pa
			)
			SELECT p.*
			FROM dbo.Products p
			LEFT JOIN LatestApproval la ON p.ProductID = la.ProductID AND la.rn = 1
			WHERE p.ShopID = :shopId
			  AND (:status IS NULL OR p.Status = :status)
			  AND (:categoryId IS NULL OR p.CategoryID = :categoryId)
			  AND (:search IS NULL OR LOWER(p.ProductName) LIKE LOWER('%' + :search + '%'))
			  AND (
			      :approvalStatus IS NULL
			      OR :approvalStatus = ''
			      OR (:approvalStatus = 'Pending' AND la.LatestApprovalStatus = 'Pending')
			      OR (:approvalStatus = 'Rejected' AND la.LatestApprovalStatus = 'Rejected')
			      OR (:approvalStatus = 'Approved' AND (la.LatestApprovalStatus = 'Approved' OR la.LatestApprovalStatus IS NULL))
			  )
			ORDER BY p.ProductID DESC
			""", countQuery = """
			WITH LatestApproval AS (
			    SELECT
			        pa.ProductID,
			        pa.Status AS LatestApprovalStatus,
			        ROW_NUMBER() OVER(PARTITION BY pa.ProductID ORDER BY pa.RequestedAt DESC) as rn
			    FROM dbo.ProductApprovals pa
			)
			SELECT COUNT(p.ProductID)
			FROM dbo.Products p
			LEFT JOIN LatestApproval la ON p.ProductID = la.ProductID AND la.rn = 1
			WHERE p.ShopID = :shopId
			  AND (:status IS NULL OR p.Status = :status)
			  AND (:categoryId IS NULL OR p.CategoryID = :categoryId)
			  AND (:search IS NULL OR LOWER(p.ProductName) LIKE LOWER('%' + :search + '%'))
			  AND (
			      :approvalStatus IS NULL
			      OR :approvalStatus = ''
			      OR (:approvalStatus = 'Pending' AND la.LatestApprovalStatus = 'Pending')
			      OR (:approvalStatus = 'Rejected' AND la.LatestApprovalStatus = 'Rejected')
			      OR (:approvalStatus = 'Approved' AND (la.LatestApprovalStatus = 'Approved' OR la.LatestApprovalStatus IS NULL))
			  )
			""", nativeQuery = true)
	Page<Product> searchShopProducts(@Param("shopId") Integer shopId, @Param("status") Byte status,
			@Param("categoryId") Integer categoryId, @Param("approvalStatus") String approvalStatus,
			@Param("search") String search, Pageable pageable);

	// ===== Customer Context Queries (ĐÃ SỬA để thêm ShopId Filter) =====

	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" + "p, " + "CAST(COALESCE(p.soldCount, 0) AS long), "
			+ "(SELECT MAX(pp.discountPercentage) " + " FROM PromotionProduct pp JOIN pp.promotion pr "
			+ " WHERE pp.product = p AND pr.status = 1 " + 
			" AND pr.promotionType = 'PRODUCT' " + 
			" AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" + ") "
			+ "FROM Product p WHERE p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)", 
			countQuery = "SELECT COUNT(p) FROM Product p WHERE p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)") 
	Page<ProductSaleDTO> findProductSaleData(@Param("shopId") Integer shopId, Pageable pageable); 

	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" + "p, " + "CAST(COALESCE(p.soldCount, 0) AS long), "
			+ "(SELECT MAX(pp.discountPercentage) " + " FROM PromotionProduct pp JOIN pp.promotion pr "
			+ " WHERE pp.product = p " + " AND pr.status = 1 AND pr.promotionType = 'PRODUCT' " + 
			" AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" + ") " + "FROM Product p "
			+ "WHERE p.category = :category AND p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)", 
			countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category = :category AND p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)") 
	Page<ProductSaleDTO> findProductSaleDataByCategory(@Param("category") Category category, @Param("shopId") Integer shopId, Pageable pageable); 

	// ✅ SỬA: Thêm điều kiện KM hợp lệ vào subquery
	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" + "p, " + "CAST(COALESCE(p.soldCount, 0) AS long), "
			+ "(SELECT MAX(pp.discountPercentage) " + " FROM PromotionProduct pp JOIN pp.promotion pr "
			+ " WHERE pp.product = p AND pr.status = 1 AND pr.promotionType = 'PRODUCT' " + // ✅ Thêm điều kiện KM
			" AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" + ") "
			+ "FROM Product p " + "WHERE p.productID = :id AND p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)") 
	Optional<ProductSaleDTO> findProductSaleDataById(@Param("id") Integer id, @Param("shopId") Integer shopId); 

	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" + "p, " + "CAST(COALESCE(p.soldCount, 0) AS long), "
			+ "(SELECT MAX(pp.discountPercentage) " + " FROM PromotionProduct pp JOIN pp.promotion pr "
			+ " WHERE pp.product = p AND pr.status = 1 AND pr.promotionType = 'PRODUCT' " + 
			" AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" + ") "
			+ "FROM Product p "
			+ "WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)", 
			countQuery = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)") 
	Page<ProductSaleDTO> findProductSaleDataByKeyword(@Param("keyword") String keyword, @Param("shopId") Integer shopId, Pageable pageable); 
	
	// ✅ SỬA: Trả về ProductSaleDTO (để có data khuyến mãi/bán hàng)
	@Query("SELECT new com.alotra.model.ProductSaleDTO(" +
		       "p, " +
		       "CAST(COALESCE(p.soldCount, 0) AS long), " +
		       "(SELECT MAX(pp.discountPercentage) " +
		       " FROM PromotionProduct pp JOIN pp.promotion pr " +
		       " WHERE pp.product = p AND pr.status = 1 " +
		       " AND pr.promotionType = 'PRODUCT' " +
		       " AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
		       ") " +
		       "FROM Product p " +
		       "WHERE p.shop.shopId = :shopId AND p.status = 1 " + 
		       "ORDER BY p.createdAt DESC")
	Page<ProductSaleDTO> findActiveProductsByShop(
		        @Param("shopId") Integer shopId,
		        Pageable pageable);
	
	// ✅ SỬA: Giữ nguyên Product cho list (thường dùng cho các chức năng liên kết)
	@Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.status = 1 ORDER BY p.productName") 
    List<Product> findActiveProductsByShop(@Param("shopId") Integer shopId);

	// Sửa kiểu dữ liệu tham số từ Integer sang Byte (Status là Byte trong entity)
	Page<Product> findByStatus(Byte status, Pageable pageable); 
	
	@Query("SELECT p.productID FROM Product p WHERE p.shop.shopId IN :shopIds")
    List<Integer> findProductIdsByShopIds(List<Integer> shopIds);


}