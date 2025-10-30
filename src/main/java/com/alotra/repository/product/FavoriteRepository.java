package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Favorite;

import jakarta.transaction.Transactional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    
    List<Favorite> findByUser_IdOrderByCreatedAtDesc(Integer userId);
    
    Optional<Favorite> findByUser_IdAndProduct_ProductID(Integer userId, Integer productId);
    
    Boolean existsByUser_IdAndProduct_ProductID(Integer userId, Integer productId);
    
    @Transactional 
    void deleteByUser_IdAndProduct_ProductID(Integer userId, Integer productId);
    
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.product.productID = :productId")
    Long countByProductId(@Param("productId") Integer productId);
}