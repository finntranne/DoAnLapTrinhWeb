package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    
    List<Favorite> findByUser_UserIDOrderByCreatedAtDesc(Integer userId);
    
    Optional<Favorite> findByUser_UserIDAndProduct_ProductID(Integer userId, Integer productId);
    
    Boolean existsByUser_UserIDAndProduct_ProductID(Integer userId, Integer productId);
    
    void deleteByUser_UserIDAndProduct_ProductID(Integer userId, Integer productId);
    
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.product.productID = :productId")
    Long countByProductId(@Param("productId") Integer productId);
}