package com.alotra.service.product;

import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.model.ProductSaleDTO;
import com.alotra.repository.product.ProductRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    
    // Cập nhật: Thêm shopId vào tham số
    public Page<ProductSaleDTO> findProductSaleData(Integer shopId, Pageable pageable) {
        return productRepository.findProductSaleData(shopId, pageable);
    }
    
    public Page<ProductSaleDTO> findProductSaleDataByCategory(Category category, Integer shopId, Pageable pageable) {
        return productRepository.findProductSaleDataByCategory(category, shopId, pageable);
    }
    
    public Optional<ProductSaleDTO> findProductSaleDataById(Integer id, Integer shopId) {
        return productRepository.findProductSaleDataById(id, shopId);
    }
    
    public Page<ProductSaleDTO> findProductSaleDataByKeyword(String keyword, Integer shopId, Pageable pageable){
    	return productRepository.findProductSaleDataByKeyword(keyword, shopId, pageable);
    }
    
    public Page<Product> findAll(Pageable pageable) {
		return productRepository.findAll(pageable);
	}
    
    public Optional<Product> findById(Integer id) {
    	return productRepository.findById(id);
    }

    // ✅ SỬA: Chuyển '1' thành '(byte) 1' để khớp với kiểu Byte trong Product Entity
	public Page<Product> findAllApproved(Pageable pageable) {
		return productRepository.findByStatus((byte) 1, pageable);
	}
	
	public List<Product> findAllActive(){
		return productRepository.findAll();
	}

	public List<Integer> findProductIdsByShopIds(List<Integer> applicableShopIds) {
		return productRepository.findProductIdsByShopIds(applicableShopIds);
	}
}