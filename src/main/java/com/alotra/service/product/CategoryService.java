package com.alotra.service.product;


import com.alotra.entity.product.Category;
import com.alotra.repository.product.CategoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
