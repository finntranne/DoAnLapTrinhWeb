//<<<<<<< HEAD
//package com.alotra.repository.product; // Hoặc package repository của bạn
//
//import com.alotra.entity.product.ProductVariant;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
//    // Kế thừa JpaRepository là đủ, đã có findById()
//}
//=======
//package com.alotra.repository.product;
//
//import java.util.List;
//import java.util.Optional;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import com.alotra.entity.product.ProductVariant;
//
//@Repository
//public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
//    
//    List<ProductVariant> findByProduct_ProductID(Integer productId);
//    
//    Optional<ProductVariant> findByProduct_ProductIDAndSize_SizeID(Integer productId, Integer sizeId);
//    
//    Optional<ProductVariant> findBySku(String sku);
//    
//    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.productID = :productId " +
//           "ORDER BY pv.price ASC")
//    List<ProductVariant> findByProductIdOrderByPriceAsc(@Param("productId") Integer productId);
//    
//    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stock < :threshold")
//    List<ProductVariant> findLowStockVariants(@Param("threshold") Integer threshold);
//}
//>>>>>>> lam

package com.alotra.repository.product; // Giữ package này

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> { // Khớp Entity

    // --- Giữ lại các phương thức từ nhánh lam ---
    List<ProductVariant> findByProduct_ProductID(Integer productId);

    Optional<ProductVariant> findByProduct_ProductIDAndSize_SizeID(Integer productId, Integer sizeId);

    Optional<ProductVariant> findBySku(String sku);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.productID = :productId " +
           "ORDER BY pv.price ASC")
    List<ProductVariant> findByProductIdOrderByPriceAsc(@Param("productId") Integer productId);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stock < :threshold")
    List<ProductVariant> findLowStockVariants(@Param("threshold") Integer threshold);
    
}