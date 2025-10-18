package com.alotra.repository.product;

import com.alotra.model.ProductSaleDTO;
import com.alotra.entity.product.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(p, CAST(p.soldCount AS long), MAX(pp.discountPercentage)) " +
            "FROM Product p " +
            "LEFT JOIN p.promotionProducts pp " +
            // Lọc các promotion đang hoạt động
            "LEFT JOIN pp.promotion promo ON promo.status = 1 AND CURRENT_DATE BETWEEN promo.startDate AND promo.endDate " +
            "WHERE p.soldCount > 10 " +
            "GROUP BY p " + // Nhóm theo sản phẩm để đảm bảo mỗi sản phẩm chỉ 1 dòng
            "ORDER BY p.soldCount DESC",
    
    // countQuery là bắt buộc khi dùng GROUP BY trong câu query chính
    countQuery = "SELECT COUNT(p) " +
                 "FROM Product p " +
                 "WHERE p.soldCount > 10")
Page<ProductSaleDTO> findBestSellingProducts(Pageable pageable);
	
	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(p, CAST(p.soldCount AS long), MAX(pp.discountPercentage)) " +
            "FROM Product p " +
            "LEFT JOIN p.promotionProducts pp " +
            "LEFT JOIN pp.promotion promo ON promo.status = 1 AND CURRENT_DATE BETWEEN promo.startDate AND promo.endDate " +
            "GROUP BY p " + // Nhóm theo sản phẩm
            "ORDER BY p.createdAt DESC", // Chỉ thay đổi dòng này
    
    countQuery = "SELECT COUNT(p) FROM Product p")
Page<ProductSaleDTO> findNewestProductsWithSale(Pageable pageable);
}