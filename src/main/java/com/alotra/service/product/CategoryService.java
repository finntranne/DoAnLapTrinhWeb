package com.alotra.service.product;


import com.alotra.entity.product.Category;
import com.alotra.entity.user.User;
import com.alotra.repository.product.CategoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAllWithProducts();
    }
    
    public Optional<Category> findById(Integer categoryId) {
        return categoryRepository.findById(categoryId);
    }
    
    public Optional<Category> findByCategoryName(String categoryName) {
        return categoryRepository.findByCategoryNameIgnoreCase(categoryName);
    }
    
    public Page<Category> findAll(Pageable pageable) {
		return categoryRepository.findAll(pageable);
	}
    
    public Category save(Category cate) {
		return categoryRepository.save(cate);
	}
    
    public Page<Category> searchCategories(String keyword, int page) {
		Pageable pageable = PageRequest.of(page - 1, 10);
        if (keyword == null || keyword.trim().isEmpty()) {
            return categoryRepository.findAll(pageable);
        }
        return categoryRepository.findByCategoryNameContaining(keyword, pageable);
	}
    
    public boolean existsByCategoryName(String categoryName) {
		return categoryRepository.existsByCategoryName(categoryName);
	}
    
    public List<Category> searchCategories(String categoryName, Integer status, int page) {
		if (categoryName != null && categoryName.isBlank()) categoryName = null;
	    Pageable pageable = PageRequest.of(page - 1, 10);

	    Page<Category> result = categoryRepository.searchCategories(categoryName, status, pageable);
	    
	    return result.getContent();
	}
    
    public int getTotalPages(String categoryName, Integer status) {
		 Pageable pageable = PageRequest.of(0, 10);
		
	    Page<Category> result = categoryRepository.searchCategories(categoryName, status, pageable);
	    return result.getTotalPages();
	}
}
