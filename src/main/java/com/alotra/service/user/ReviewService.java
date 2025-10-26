package com.alotra.service.user;

import com.alotra.entity.product.Product;
import com.alotra.entity.product.Review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    Page<Review> findByProduct(Product product, Pageable pageable);
}