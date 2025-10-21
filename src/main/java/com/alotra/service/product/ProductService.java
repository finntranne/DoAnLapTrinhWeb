package com.alotra.service.product;

import com.alotra.entity.product.Category;
import com.alotra.model.ProductSaleDTO;
import com.alotra.repository.product.ProductRepository;

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
}
