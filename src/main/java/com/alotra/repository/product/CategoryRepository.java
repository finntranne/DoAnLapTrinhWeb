package com.alotra.repository.product;


import com.alotra.entity.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    // Thêm câu lệnh này để lấy tất cả category cùng với product của chúng trong 1 query
    @Query("SELECT c FROM Category c JOIN FETCH c.products")
    List<Category> findAllWithProducts();
}
