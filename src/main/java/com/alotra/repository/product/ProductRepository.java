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

//	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(p, CAST(p.soldCount AS long), MAX(pp.discountPercentage)) " +
//            "FROM Product p " +
//            "LEFT JOIN p.promotionProducts pp " +
//            // Lọc các promotion đang hoạt động
//            "LEFT JOIN pp.promotion promo ON promo.status = 1 AND CURRENT_DATE BETWEEN promo.startDate AND promo.endDate " +
//            "WHERE p.soldCount > 10 " +
//            "GROUP BY p " + // Nhóm theo sản phẩm để đảm bảo mỗi sản phẩm chỉ 1 dòng
//            "ORDER BY p.soldCount DESC",
//    
//    // countQuery là bắt buộc khi dùng GROUP BY trong câu query chính
//    countQuery = "SELECT COUNT(p) " +
//                 "FROM Product p " +
//                 "WHERE p.soldCount > 10")
//Page<ProductSaleDTO> findBestSellingProducts(Pageable pageable);
//	
//	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(p, CAST(p.soldCount AS long), MAX(pp.discountPercentage)) " +
//            "FROM Product p " +
//            "LEFT JOIN p.promotionProducts pp " +
//            "LEFT JOIN pp.promotion promo ON promo.status = 1 AND CURRENT_DATE BETWEEN promo.startDate AND promo.endDate " +
//            "GROUP BY p " + // Nhóm theo sản phẩm
//            "ORDER BY p.createdAt DESC", // Chỉ thay đổi dòng này
//    
//    countQuery = "SELECT COUNT(p) FROM Product p")
//Page<ProductSaleDTO> findNewestProductsWithSale(Pageable pageable);
//	
//	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" +
//	           "p, " +
//	           "COALESCE(SUM(od.quantity), 0L), " +
//	           "(SELECT MAX(pp.discountPercentage) " + // <-- SỬA 1: Đổi từ pr.discountPercentage
//	           " FROM PromotionProduct pp JOIN pp.promotion pr " +
//	           " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
//	    ") " +
//	    "FROM Product p " +
//	    "LEFT JOIN p.productVariants pv " +
//	    "LEFT JOIN pv.orderDetails od " +
//	    "GROUP BY p", // <-- SỬA 2: Xóa 'ORDER BY' khỏi đây
//	    
//	    // SỬA 3: Thêm countQuery rõ ràng vì dùng Page và GROUP BY
//	    countQuery = "SELECT COUNT(p) FROM Product p")
//	    Page<ProductSaleDTO> findTopRatedProducts(Pageable pageable);
	
	@Query(value = "SELECT new com.alotra.model.ProductSaleDTO(" +
	           "p, " +
	           // SỬA LỖI: Dùng trường p.soldCount đã cache thay vì SUM()
	           "CAST(COALESCE(p.soldCount, 0) AS long), " +
	           
	           // Giữ nguyên subquery để lấy discount (cách này hiệu quả)
	           "(SELECT MAX(pp.discountPercentage) " +
	           " FROM PromotionProduct pp JOIN pp.promotion pr " +
	           " WHERE pp.product = p AND pr.startDate <= CURRENT_TIMESTAMP AND pr.endDate >= CURRENT_TIMESTAMP)" +
	    ") " +
	    // Bỏ JOIN và GROUP BY không cần thiết, giúp query nhanh hơn
	    "FROM Product p", 
	    
	    // Count query đơn giản
	    countQuery = "SELECT COUNT(p) FROM Product p")
	Page<ProductSaleDTO> findProductSaleData(Pageable pageable);
}