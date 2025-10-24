package com.alotra.repository.product;

import com.alotra.model.ProductSaleDTO;
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" +
	           "p, " +
	           // SỬA LỖI: Dùng trường p.soldCount đã cache thay vì SUM()
	           "CAST(COALESCE(p.soldCount, 0) AS long), " +
	           
	           // Giữ nguyên subquery để lấy discount (cách này hiệu quả)
	           "(SELECT MAX(pp.discountPercentage) " +
	           " FROM PromotionProduct pp JOIN pp.promotion pr " +
	           " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
	    ") " +
	    // Bỏ JOIN và GROUP BY không cần thiết, giúp query nhanh hơn
	    "FROM Product p", 
	    
	    // Count query đơn giản
	    countQuery = "SELECT COUNT(p) FROM Product p")
	Page<ProductSaleDTO> findProductSaleData(Pageable pageable);
	
	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" +
            "p, " +
            "CAST(COALESCE(p.soldCount, 0) AS long), " +
            "(SELECT MAX(pp.discountPercentage) " +
            " FROM PromotionProduct pp JOIN pp.promotion pr " +
            " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
     ") " +
     "FROM Product p " +
     "WHERE p.category = :category", // <-- Thêm điều kiện lọc
 
 countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category = :category") // <-- Thêm điều kiện
 Page<ProductSaleDTO> findProductSaleDataByCategory(@Param("category") Category category, Pageable pageable);
	
	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" +
            "p, " +
            "CAST(COALESCE(p.soldCount, 0) AS long), " +
            "(SELECT MAX(pp.discountPercentage) " +
            " FROM PromotionProduct pp JOIN pp.promotion pr " +
            " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
     ") " +
     "FROM Product p " +
     "WHERE p.productId = :id") // <-- Lọc theo ID
 Optional<ProductSaleDTO> findProductSaleDataById(@Param("id") Integer id);
	
	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" +
            "p, " +
            "CAST(COALESCE(p.soldCount, 0) AS long), " +
            "(SELECT MAX(pp.discountPercentage) " +
            " FROM PromotionProduct pp JOIN pp.promotion pr " +
            " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
     ") " +
     "FROM Product p " +
     // Thêm điều kiện tìm kiếm
     "WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))",
     
     countQuery = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ProductSaleDTO> findProductSaleDataByKeyword(@Param("keyword") String keyword, Pageable pageable);
}