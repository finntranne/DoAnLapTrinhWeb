package com.alotra.repository.product; // Hoặc package repository của bạn

import com.alotra.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    // Kế thừa JpaRepository là đủ, đã có findById()
}