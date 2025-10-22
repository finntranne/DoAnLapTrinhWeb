package com.alotra.service.product.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.product.Product;
import com.alotra.repository.product.ProductRepository;
import com.alotra.service.product.ProductService;

@Service
public class ProductServiceImpl implements ProductService{
	
	@Autowired
	ProductRepository productRepository;

	@Override
	public List<Product> getAllProducts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<Product> getProductById(int id) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Product saveProduct(Product product) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product updateProduct(int id, Product product) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteProduct(int id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Product> getProductsByCategory(int categoryId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product> getProductsByShop(int shopId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Product> findAll(Pageable pageable) {
		return productRepository.findAll(pageable);
	}

	@Override
	public Optional<Product> findById(Integer id) {
		return productRepository.findById(id);
	}

	@Override
	public List<Product> searchProducts(String productName, String categoryName, String shopName, String status,
			int page) {
		if (productName != null && productName.isBlank()) productName = null;
	    if (categoryName != null && categoryName.isBlank()) categoryName = null;
	    if (shopName != null && shopName.isBlank()) shopName = null;
	    if (status != null && status.isBlank()) status = null;
	    
		Pageable pageable = PageRequest.of(page - 1, 10);
	    Page<Product> result = productRepository.searchProducts(productName, categoryName, shopName, status, pageable);
	    return result.getContent();
	}

	@Override
	public int getTotalPages(String productName, String categoryName, String shopName, String status) {
		
		Pageable pageable = PageRequest.of(0, 10);
	    Page<Product> result = productRepository.searchProducts(productName, categoryName, shopName, status, pageable);
	    return result.getTotalPages();
	}

}
