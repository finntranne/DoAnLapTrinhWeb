package com.alotra.service.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.alotra.entity.product.Product;

public interface ProductService {

	Optional<Product> findById(Integer id);
	
	List<Product> getAllProducts();
    Optional<Product> getProductById(int id);
    Product saveProduct(Product product);
    Product updateProduct(int id, Product product);
    void deleteProduct(int id);
    List<Product> getProductsByCategory(int categoryId);
    List<Product> getProductsByShop(int shopId);
    
    
    Page<Product> findAll(Pageable pageable);
    
    public List<Product> searchProducts(String productName, String categoryName, String shopName, String status, int page);
	public int getTotalPages(String productName, String categoryName, String shopName, String status);
}
