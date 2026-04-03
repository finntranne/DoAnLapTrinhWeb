package com.alotra.service.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.alotra.entity.product.Product;
import com.alotra.entity.product.Review;

public interface ReviewService {

    Page<Review> findByProduct(Product product, Pageable pageable);
}
