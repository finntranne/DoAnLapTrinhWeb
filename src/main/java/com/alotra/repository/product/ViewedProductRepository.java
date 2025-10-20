package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.ViewedProduct;

@Repository
public interface ViewedProductRepository extends JpaRepository<ViewedProduct, Integer> {
    
    List<ViewedProduct> findByUser_UserIDOrderByLastViewedAtDesc(Integer userId);
    
    Optional<ViewedProduct> findByUser_UserIDAndProduct_ProductID(Integer userId, Integer productId);
    
    @Query("SELECT vp FROM ViewedProduct vp " +
           "WHERE vp.user.userID = :userId " +
           "ORDER BY vp.lastViewedAt DESC")
    Page<ViewedProduct> findRecentlyViewedProducts(@Param("userId") Integer userId, Pageable pageable);
}

