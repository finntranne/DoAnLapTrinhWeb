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
    
    public Page<ProductSaleDTO> findProductSaleDataPaginated(Pageable pageable) {
        return productRepository.findProductSaleData(pageable);
    }
    
    public Page<ProductSaleDTO> findProductSaleDataByCategoryPaginated(Category category, Pageable pageable) {
        return productRepository.findProductSaleDataByCategory(category, pageable);
    }
    
    public Optional<ProductSaleDTO> findProductSaleDataById(Integer id) {
        return productRepository.findProductSaleDataById(id);
    }
    
    public Page<ProductSaleDTO> findProductSaleDataByKeyword(String keyword, Pageable pageable){
    	return productRepository.findProductSaleDataByKeyword(keyword, pageable);
    }
    
    public Page<Product> findAll(Pageable pageable) {
		return productRepository.findAll(pageable);
	}
    
    public Optional<Product> findById(Integer id) {
    	return productRepository.findById(id);
    }

	public Page<Product> findAllApproved(Pageable pageable) {
		return productRepository.findByStatus(1, pageable);
	}
	
	public List<Product> findAllActive(){
		return productRepository.findAll();
	}

	public List<Integer> findProductIdsByShopIds(List<Integer> applicableShopIds) {
		return productRepository.findProductIdsByShopIds(applicableShopIds);
	}
}
