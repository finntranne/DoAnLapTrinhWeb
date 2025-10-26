//package com.alotra.repository.user;
//
//import com.alotra.entity.product.Product;
//import com.alotra.entity.user.Review;
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface ReviewRepository extends JpaRepository<Review, Integer> {
//    
//    // Lấy review theo sản phẩm, sắp xếp mới nhất
//    Page<Review> findByProductOrderByReviewDateDesc(Product product, Pageable pageable);
//}