package com.alotra.service.product;

import com.alotra.entity.product.Product;
import com.alotra.model.ProductSaleDTO;
import com.alotra.repository.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    
    public Page<ProductSaleDTO> findProductSaleDataPaginated(Pageable pageable) {
        return productRepository.findProductSaleData(pageable);
    }
}
