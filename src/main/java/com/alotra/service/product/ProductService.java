package com.alotra.service.product;

import com.alotra.model.ProductSaleDTO;
import com.alotra.repository.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<ProductSaleDTO> getTopProducts() { // Sửa kiểu trả về
        return productRepository.findBestSellingProducts();
    }
}