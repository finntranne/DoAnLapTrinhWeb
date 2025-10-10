package com.alotra.service.product;


import com.alotra.entity.product.Product;
import com.alotra.repository.product.ProductRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getTopProducts() {
        // Tạm thời lấy tất cả sản phẩm, sau này có thể thêm logic lấy sản phẩm bán chạy
        return productRepository.findAll();
    }
}