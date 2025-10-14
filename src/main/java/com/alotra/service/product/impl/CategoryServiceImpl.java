package com.alotra.service.product.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.product.Category;
import com.alotra.repository.product.CategoryRepository;
import com.alotra.service.product.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService{
	
	@Autowired
	private CategoryRepository categoryRepository;

	@Override
	public Optional<Category> findById(Integer id) {
		return categoryRepository.findById(id);
	}

	@Override
	public Category save(Category cate) {
		return categoryRepository.save(cate);
	}

	@Override
	public void deleteById(Integer id) {
		categoryRepository.deleteById(id);
	}

	@Override
	public boolean existsById(Integer id) {
		return categoryRepository.existsById(id);
	}

	@Override
	public long count() {
		return categoryRepository.count();
	}

	@Override
	public Page<Category> findAll(Pageable pageable) {
		return categoryRepository.findAll(pageable);
	}

	@Override
	public List<Category> findByCategoryNameContaining(String categoryname) {
		return categoryRepository.findByCategoryNameContaining(categoryname);
	}

	@Override
	public Page<Category> findByCategoryNameContaining(String categoryname, Pageable pageable) {
		return categoryRepository.findByCategoryNameContaining(categoryname, pageable);
	}

	@Override
	public Optional<Category> findByCategoryName(String categoryname) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Category updateCategory(Integer id, Category cate) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
