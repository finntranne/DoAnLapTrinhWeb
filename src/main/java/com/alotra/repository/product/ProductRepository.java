package com.alotra.repository.product;

import com.alotra.model.ProductSaleDTO;
import com.alotra.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query("SELECT new com.alotra.model.ProductSaleDTO(p, SUM(od.quantity), pp.discountPercentage) " +
           "FROM Product p " +
           "JOIN p.productVariants v " +
           "JOIN v.orderDetails od " +
           "JOIN od.order o " +
           // LEFT JOIN để lấy cả những sản phẩm không có khuyến mãi
           "LEFT JOIN p.promotionProducts pp " +
           "LEFT JOIN pp.promotion promo ON promo.status = 1 AND CURRENT_DATE BETWEEN promo.startDate AND promo.endDate " +
           "WHERE o.orderStatus = 'Completed' " +
           "GROUP BY p, pp.discountPercentage " + // Thêm discount vào GROUP BY
           "ORDER BY SUM(od.quantity) DESC")
    List<ProductSaleDTO> findBestSellingProducts();
}