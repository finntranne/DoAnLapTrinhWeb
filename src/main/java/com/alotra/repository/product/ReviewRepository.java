package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    
    List<Review> findByProduct_ProductIDOrderByReviewDateDesc(Integer productId);
    
    Page<Review> findByProduct_ProductIDOrderByReviewDateDesc(Integer productId, Pageable pageable);
    
    @Query("SELECT r FROM Review r " +
           "WHERE r.product.productID = :productId " +
           "AND (:rating IS NULL OR r.rating = :rating) " +
           "ORDER BY r.reviewDate DESC")
    Page<Review> findByProductIdAndRating(
        @Param("productId") Integer productId,
        @Param("rating") Integer rating,
        Pageable pageable
    );
    
    Optional<Review> findByOrderDetail_OrderDetailID(Integer orderDetailId);
    
    Boolean existsByOrderDetail_OrderDetailID(Integer orderDetailId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productID = :productId")
    Double calculateAverageRating(@Param("productId") Integer productId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.productID = :productId")
    Long countByProductId(@Param("productId") Integer productId);
}
