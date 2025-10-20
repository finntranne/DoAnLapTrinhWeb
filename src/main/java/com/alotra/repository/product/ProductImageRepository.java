package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.ProductImage;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    
    List<ProductImage> findByProduct_ProductIDOrderByDisplayOrderAsc(Integer productId);
    
    Optional<ProductImage> findByProduct_ProductIDAndIsPrimary(Integer productId, Boolean isPrimary);
    
    void deleteByProduct_ProductID(Integer productId);
}
