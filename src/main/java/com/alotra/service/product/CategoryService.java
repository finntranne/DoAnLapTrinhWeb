package com.alotra.service.product;


import com.alotra.entity.product.Category;
import com.alotra.repository.product.CategoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAllWithProducts();
    }
}
