package com.alotra.repository.product; // Hoặc package repository của bạn

import com.alotra.entity.product.Topping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToppingRepository extends JpaRepository<Topping, Integer> {
    // Kế thừa JpaRepository là đủ, đã có findAllById()
}