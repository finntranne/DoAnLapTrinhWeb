//package com.alotra.repository.product;
//
//<<<<<<< HEAD
//import com.alotra.model.ProductSaleDTO;
//import com.alotra.entity.product.Category;
//import com.alotra.entity.product.Product;
//
//import java.util.Optional;
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//
//@Repository
//public interface ProductRepository extends JpaRepository<Product, Integer> {
//	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" +
//	           "p, " +
//	           // SỬA LỖI: Dùng trường p.soldCount đã cache thay vì SUM()
//	           "CAST(COALESCE(p.soldCount, 0) AS long), " +
//	           
//	           // Giữ nguyên subquery để lấy discount (cách này hiệu quả)
//	           "(SELECT MAX(pp.discountPercentage) " +
//	           " FROM PromotionProduct pp JOIN pp.promotion pr " +
//	           " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
//	    ") " +
//	    // Bỏ JOIN và GROUP BY không cần thiết, giúp query nhanh hơn
//	    "FROM Product p", 
//	    
//	    // Count query đơn giản
//	    countQuery = "SELECT COUNT(p) FROM Product p")
//	Page<ProductSaleDTO> findProductSaleData(Pageable pageable);
//	
//	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" +
//            "p, " +
//            "CAST(COALESCE(p.soldCount, 0) AS long), " +
//            "(SELECT MAX(pp.discountPercentage) " +
//            " FROM PromotionProduct pp JOIN pp.promotion pr " +
//            " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
//     ") " +
//     "FROM Product p " +
//     "WHERE p.category = :category", // <-- Thêm điều kiện lọc
// 
// countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category = :category") // <-- Thêm điều kiện
// Page<ProductSaleDTO> findProductSaleDataByCategory(@Param("category") Category category, Pageable pageable);
//	
//	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" +
//            "p, " +
//            "CAST(COALESCE(p.soldCount, 0) AS long), " +
//            "(SELECT MAX(pp.discountPercentage) " +
//            " FROM PromotionProduct pp JOIN pp.promotion pr " +
//            " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
//     ") " +
//     "FROM Product p " +
//     "WHERE p.productId = :id") // <-- Lọc theo ID
// Optional<ProductSaleDTO> findProductSaleDataById(@Param("id") Integer id);
//	
//	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" +
//            "p, " +
//            "CAST(COALESCE(p.soldCount, 0) AS long), " +
//            "(SELECT MAX(pp.discountPercentage) " +
//            " FROM PromotionProduct pp JOIN pp.promotion pr " +
//            " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
//     ") " +
//     "FROM Product p " +
//     // Thêm điều kiện tìm kiếm
//     "WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))",
//     
//     countQuery = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
//    Page<ProductSaleDTO> findProductSaleDataByKeyword(@Param("keyword") String keyword, Pageable pageable);
//=======
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import com.alotra.entity.product.Product;
//
//@Repository
//public interface ProductRepository extends JpaRepository<Product, Integer> {
//    
//    Page<Product> findByShop_ShopIdOrderByCreatedAtDesc(Integer shopId, Pageable pageable);
//    
//    @Query("SELECT COUNT(p) FROM Product p WHERE p.shop.shopId = :shopId AND p.status = :status")
//    Long countByShopIdAndStatus(@Param("shopId") Integer shopId, @Param("status") Byte status);
//    
//    @Query("SELECT p FROM Product p " +
//           "WHERE p.shop.shopId = :shopId " +
//           "AND (:status IS NULL OR p.status = :status) " +
//           "AND (:search IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')) " +
//           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
//           "ORDER BY p.createdAt DESC")
//    Page<Product> searchShopProducts(
//        @Param("shopId") Integer shopId,
//        @Param("status") Byte status,
//        @Param("search") String search,
//        Pageable pageable
//    );
//    
//    @Query("SELECT p FROM Product p " +
//           "WHERE p.status = 1 " +
//           "AND (:categoryId IS NULL OR p.category.categoryID = :categoryId) " +
//           "ORDER BY p.soldCount DESC")
//    Page<Product> findBestSellingProducts(@Param("categoryId") Integer categoryId, Pageable pageable);
//    
//    @Query("SELECT p FROM Product p " +
//           "WHERE p.status = 1 " +
//           "AND (:categoryId IS NULL OR p.category.categoryID = :categoryId) " +
//           "ORDER BY p.createdAt DESC")
//    Page<Product> findNewestProducts(@Param("categoryId") Integer categoryId, Pageable pageable);
//    
//    @Query("SELECT p FROM Product p " +
//           "WHERE p.status = 1 " +
//           "AND p.totalReviews >= 5 " +
//           "AND (:categoryId IS NULL OR p.category.categoryID = :categoryId) " +
//           "ORDER BY p.averageRating DESC, p.totalReviews DESC")
//    Page<Product> findTopRatedProducts(@Param("categoryId") Integer categoryId, Pageable pageable);
//
//    @Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId " +
//            "AND (:status IS NULL OR p.status = :status) " +
//            // *** ADD CATEGORY FILTER ***
//            "AND (:categoryId IS NULL OR p.category.categoryID = :categoryId) " +
//            "AND (:search IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
//     Page<Product> searchShopProducts(
//             @Param("shopId") Integer shopId,
//             @Param("status") Byte status,
//             // *** ADD categoryId PARAMETER ***
//             @Param("categoryId") Integer categoryId,
//             @Param("search") String search,
//             Pageable pageable);
//>>>>>>> lam
//}
package com.alotra.repository.product;

import com.alotra.model.ProductSaleDTO;
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductApproval;

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

	// ===== Customer Context Queries =====

	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" + "p, " + "CAST(COALESCE(p.soldCount, 0) AS long), "
			+ "(SELECT MAX(pp.discountPercentage) " + " FROM PromotionProduct pp JOIN pp.promotion pr "
			+ " WHERE pp.product = p AND pr.status = 1 " + // KM phải active
			" AND pr.promotionType = 'PRODUCT' " + // *** CHỈ LẤY KM SẢN PHẨM ***
			" AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" + ") "
			+ "FROM Product p", countQuery = "SELECT COUNT(p) FROM Product p")
	Page<ProductSaleDTO> findProductSaleData(Pageable pageable);

	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" + "p, " + "CAST(COALESCE(p.soldCount, 0) AS long), "
			+ "(SELECT MAX(pp.discountPercentage) " + " FROM PromotionProduct pp JOIN pp.promotion pr "
			+ " WHERE pp.product = p " + " AND pr.status = 1 AND pr.promotionType = 'PRODUCT' " + // thêm điều kiện mới
			" AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" + ") " + "FROM Product p "
			+ "WHERE p.category = :category", countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category = :category")
	Page<ProductSaleDTO> findProductSaleDataByCategory(@Param("category") Category category, Pageable pageable);

	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" + "p, " + "CAST(COALESCE(p.soldCount, 0) AS long), "
			+ "(SELECT MAX(pp.discountPercentage) " + " FROM PromotionProduct pp JOIN pp.promotion pr "
			+ " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" + ") "
			+ "FROM Product p " + "WHERE p.productID = :id")
	Optional<ProductSaleDTO> findProductSaleDataById(@Param("id") Integer id);

	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" + "p, " + "CAST(COALESCE(p.soldCount, 0) AS long), "
			+ "(SELECT MAX(pp.discountPercentage) " + " FROM PromotionProduct pp JOIN pp.promotion pr "
			+ " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" + ") "
			+ "FROM Product p "
			+ "WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))", countQuery = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<ProductSaleDTO> findProductSaleDataByKeyword(@Param("keyword") String keyword, Pageable pageable);
	
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
	
	@Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.status = 1 ORDER BY p.productName")
    List<Product> findActiveProductsByShop(@Param("shopId") Integer shopId);

	Page<Product> findByStatus(Integer status, Pageable pageable);
	
	@Query("SELECT p.productID FROM Product p WHERE p.shop.shopId IN :shopIds")
    List<Integer> findProductIdsByShopIds(List<Integer> shopIds);


}