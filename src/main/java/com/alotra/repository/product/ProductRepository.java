package com.alotra.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    
    Page<Product> findByShop_ShopIdOrderByCreatedAtDesc(Integer shopId, Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.shop.shopId = :shopId AND p.status = :status")
    Long countByShopIdAndStatus(@Param("shopId") Integer shopId, @Param("status") Byte status);
    
    @Query("SELECT p FROM Product p " +
           "WHERE p.shop.shopId = :shopId " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:search IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<Product> searchShopProducts(
        @Param("shopId") Integer shopId,
        @Param("status") Byte status,
        @Param("search") String search,
        Pageable pageable
    );
    
    @Query("SELECT p FROM Product p " +
           "WHERE p.status = 1 " +
           "AND (:categoryId IS NULL OR p.category.categoryID = :categoryId) " +
           "ORDER BY p.soldCount DESC")
    Page<Product> findBestSellingProducts(@Param("categoryId") Integer categoryId, Pageable pageable);
    
    @Query("SELECT p FROM Product p " +
           "WHERE p.status = 1 " +
           "AND (:categoryId IS NULL OR p.category.categoryID = :categoryId) " +
           "ORDER BY p.createdAt DESC")
    Page<Product> findNewestProducts(@Param("categoryId") Integer categoryId, Pageable pageable);
    
    @Query("SELECT p FROM Product p " +
           "WHERE p.status = 1 " +
           "AND p.totalReviews >= 5 " +
           "AND (:categoryId IS NULL OR p.category.categoryID = :categoryId) " +
           "ORDER BY p.averageRating DESC, p.totalReviews DESC")
    Page<Product> findTopRatedProducts(@Param("categoryId") Integer categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId " +
            "AND (:status IS NULL OR p.status = :status) " +
            // *** ADD CATEGORY FILTER ***
            "AND (:categoryId IS NULL OR p.category.categoryID = :categoryId) " +
            "AND (:search IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
     Page<Product> searchShopProducts(
             @Param("shopId") Integer shopId,
             @Param("status") Byte status,
             // *** ADD categoryId PARAMETER ***
             @Param("categoryId") Integer categoryId,
             @Param("search") String search,
             Pageable pageable);
}