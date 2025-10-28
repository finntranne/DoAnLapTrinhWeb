//package com.alotra.repository.product;
//
//<<<<<<< HEAD
//
//import com.alotra.entity.product.Category;
//=======
//import java.util.List;
//import java.util.Optional;
//
//>>>>>>> lam
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//
//<<<<<<< HEAD
//import java.util.List;
//=======
//import com.alotra.entity.product.Category;
//>>>>>>> lam
//
//@Repository
//public interface CategoryRepository extends JpaRepository<Category, Integer> {
//    
//<<<<<<< HEAD
//    // Thêm câu lệnh này để lấy tất cả category cùng với product của chúng trong 1 query
//    @Query("SELECT c FROM Category c JOIN FETCH c.products")
//    List<Category> findAllWithProducts();
//=======
//    List<Category> findByStatus(Byte status);
//    
//    Optional<Category> findByCategoryName(String categoryName);
//    
//    @Query("SELECT c FROM Category c WHERE c.status = 1 ORDER BY c.categoryName")
//    List<Category> findAllActiveCategories();
//>>>>>>> lam
//}
package com.alotra.repository.product; // Giữ package này

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Category;


@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> { // Khớp Entity

    // --- Giữ lại các phương thức từ nhánh lam ---
    List<Category> findByStatus(Byte status);

    Optional<Category> findByCategoryName(String categoryName);

    @Query("SELECT c FROM Category c WHERE c.status = 1 ORDER BY c.categoryName")
    List<Category> findAllActiveCategories();

    // Bỏ findAllWithProducts từ HEAD (có thể gây N+1 hoặc tải quá nhiều data)
     @Query("SELECT c FROM Category c JOIN FETCH c.products")
     List<Category> findAllWithProducts();
     
     Page<Category> findByCategoryNameContaining(String keyword, Pageable pageable);
     
     boolean existsByCategoryName(String categoryName);
     
     Optional<Category> findByCategoryNameIgnoreCase(String categoryName);
     
     @Query("""
     	    SELECT c FROM Category c
     	    WHERE (:categoryName IS NULL OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :categoryName, '%')))   
     	    AND (:status IS NULL OR c.status = :status)    	     
     	""")
     	Page<Category> searchCategories(
     	        @Param("categoryName") String categoryName,   	     
     	        @Param("status") Integer status,   	       
     	        Pageable pageable
     	);
}

