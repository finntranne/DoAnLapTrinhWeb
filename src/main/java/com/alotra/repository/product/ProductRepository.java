package com.alotra.repository.product;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>{

	List<Product> findByCategory_CategoryId(int categoryId);

    List<Product> findByShop_ShopId(int shopId);

    List<Product> findByStatus(byte status);

    List<Product> findByProductNameContainingIgnoreCase(String name);

    @Query("""
            SELECT p FROM Product p
		    LEFT JOIN p.category c
		    LEFT JOIN p.shop s
		    WHERE (:productName IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :productName, '%')))
		      AND (:categoryName IS NULL OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :categoryName, '%')))
		      AND (:shopName IS NULL OR LOWER(s.shopName) LIKE LOWER(CONCAT('%', :shopName, '%')))
		      AND (:status IS NULL OR CAST(p.status AS string) = :status)
        """)
        Page<Product> searchProducts(
            @Param("productName") String productName,
            @Param("categoryName") String categoryName,
            @Param("shopName") String shopName,
            @Param("status") String status,
            Pageable pageable
        );
	
   
}
