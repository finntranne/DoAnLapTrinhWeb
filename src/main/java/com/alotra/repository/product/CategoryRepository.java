package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    List<Category> findByStatus(Byte status);
    
    Optional<Category> findByCategoryName(String categoryName);
    
    @Query("SELECT c FROM Category c WHERE c.status = 1 ORDER BY c.categoryName")
    List<Category> findAllActiveCategories();
}
