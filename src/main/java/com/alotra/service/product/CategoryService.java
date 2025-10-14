package com.alotra.service.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.alotra.entity.product.Category;


public interface CategoryService {
	
	// ===== CRUD =====
    Optional<Category> findById(Integer id);
    Category save(Category cate);
    void deleteById(Integer id);
    boolean existsById(Integer id);
    long count();
    Page<Category> findAll(Pageable pageable);

    // ===== SEARCH =====
    List<Category> findByCategoryNameContaining(String categoryname);
    Page<Category> findByCategoryNameContaining(String categoryname, Pageable pageable);

    // ===== EXTRA =====
    Optional<Category> findByCategoryName(String categoryname);
    Category updateCategory(Integer id, Category cate);

}
