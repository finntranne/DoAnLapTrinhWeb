package com.alotra.service.user.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.product.Product;
import com.alotra.entity.product.Review;
import com.alotra.repository.product.ReviewRepository;
import com.alotra.service.user.ReviewService;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Override
    public Page<Review> findByProduct(Product product, Pageable pageable) {
        return reviewRepository.findByProductOrderByReviewDateDesc(product, pageable);
    }
}