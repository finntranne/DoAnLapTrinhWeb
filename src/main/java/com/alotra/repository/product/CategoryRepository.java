package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer>{

	@Query("SELECT c FROM Category c WHERE c.categoryName = :categoryName")
    Optional<Category> getCategoryByName(@Param("categoryName") String categoryName);

    Optional<Category> findByCategoryName(String categoryName);

    Boolean existsByCategoryName(String categoryName);


    List<Category> findByCategoryNameContaining(String keyword);

    Page<Category> findByCategoryNameContaining(String keyword, Pageable pageable);
}
   
