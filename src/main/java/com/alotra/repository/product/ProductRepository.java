package com.alotra.repository.product;

import com.alotra.model.ProductSaleDTO;
import com.alotra.entity.product.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

	@Query("SELECT new com.alotra.model.ProductSaleDTO(p, CAST(p.soldCount AS long), pp.discountPercentage) " +
	           "FROM Product p " +
	           "LEFT JOIN p.promotionProducts pp " +
	           "LEFT JOIN pp.promotion promo ON promo.status = 1 AND CURRENT_DATE BETWEEN promo.startDate AND promo.endDate " +
	           "WHERE p.soldCount > 10 " +
	           "GROUP BY p, pp.discountPercentage " + 
	           "ORDER BY p.soldCount DESC")
    List<ProductSaleDTO> findBestSellingProducts();
	
	Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);
	
	List<Product> findTop10ByOrderByCreatedAtDesc();
}