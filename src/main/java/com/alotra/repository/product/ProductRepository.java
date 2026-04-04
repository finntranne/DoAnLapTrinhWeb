package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.model.ProductSaleDTO;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query("SELECT COUNT(p) FROM Product p WHERE p.shop.shopId = :shopId AND (:status IS NULL OR p.status = :status)")
    Long countByShopIdAndStatus(@Param("shopId") Integer shopId, @Param("status") Byte status);

    @Query("""
            SELECT p FROM Product p
            WHERE p.shop.shopId = :shopId
              AND (:status IS NULL OR p.status = :status)
              AND (:categoryId IS NULL OR p.category.categoryID = :categoryId)
              AND (:search IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.productID DESC
            """)
    Page<Product> searchShopProducts(@Param("shopId") Integer shopId, @Param("status") Byte status,
            @Param("categoryId") Integer categoryId, @Param("approvalStatus") String approvalStatus,
            @Param("search") String search, Pageable pageable);

    @Query(value = """
            SELECT new com.alotra.model.ProductSaleDTO(
                p,
                COALESCE((SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.variant.product = p), 0L),
                (SELECT MAX(pp.discountPercentage)
                 FROM PromotionProduct pp JOIN pp.promotion pr
                 WHERE pp.product = p
                   AND pr.status = 1
                   AND pr.promotionType = 'PRODUCT'
                   AND pr.startDate <= CURRENT_TIMESTAMP
                   AND pr.endDate >= CURRENT_TIMESTAMP),
                COALESCE((SELECT AVG(r.rating) FROM Review r WHERE r.product = p), 0.0),
                COALESCE((SELECT COUNT(r) FROM Review r WHERE r.product = p), 0L),
                COALESCE((SELECT COUNT(f) FROM Favorite f WHERE f.product = p), 0L)
            )
            FROM Product p
            WHERE p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)
            ORDER BY COALESCE((SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.variant.product = p), 0L) DESC
            """,
            countQuery = "SELECT COUNT(p) FROM Product p WHERE p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)")
    Page<ProductSaleDTO> findProductSaleData(@Param("shopId") Integer shopId, Pageable pageable);

    @Query(value = """
            SELECT new com.alotra.model.ProductSaleDTO(
                p,
                COALESCE((SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.variant.product = p), 0L),
                (SELECT MAX(pp.discountPercentage)
                 FROM PromotionProduct pp JOIN pp.promotion pr
                 WHERE pp.product = p
                   AND pr.status = 1
                   AND pr.promotionType = 'PRODUCT'
                   AND pr.startDate <= CURRENT_TIMESTAMP
                   AND pr.endDate >= CURRENT_TIMESTAMP),
                COALESCE((SELECT AVG(r.rating) FROM Review r WHERE r.product = p), 0.0),
                COALESCE((SELECT COUNT(r) FROM Review r WHERE r.product = p), 0L),
                COALESCE((SELECT COUNT(f) FROM Favorite f WHERE f.product = p), 0L)
            )
            FROM Product p
            WHERE p.category = :category
              AND p.status = 1
              AND (:shopId = 0 OR p.shop.shopId = :shopId)
            ORDER BY COALESCE((SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.variant.product = p), 0L) DESC
            """,
            countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category = :category AND p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)")
    Page<ProductSaleDTO> findProductSaleDataByCategory(@Param("category") Category category,
            @Param("shopId") Integer shopId, Pageable pageable);

    @Query("""
            SELECT new com.alotra.model.ProductSaleDTO(
                p,
                COALESCE((SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.variant.product = p), 0L),
                (SELECT MAX(pp.discountPercentage)
                 FROM PromotionProduct pp JOIN pp.promotion pr
                 WHERE pp.product = p
                   AND pr.status = 1
                   AND pr.promotionType = 'PRODUCT'
                   AND pr.startDate <= CURRENT_TIMESTAMP
                   AND pr.endDate >= CURRENT_TIMESTAMP),
                COALESCE((SELECT AVG(r.rating) FROM Review r WHERE r.product = p), 0.0),
                COALESCE((SELECT COUNT(r) FROM Review r WHERE r.product = p), 0L),
                COALESCE((SELECT COUNT(f) FROM Favorite f WHERE f.product = p), 0L)
            )
            FROM Product p
            WHERE p.productID = :id
              AND p.status = 1
              AND (:shopId = 0 OR p.shop.shopId = :shopId)
            """)
    Optional<ProductSaleDTO> findProductSaleDataById(@Param("id") Integer id, @Param("shopId") Integer shopId);

    @Query(value = """
            SELECT new com.alotra.model.ProductSaleDTO(
                p,
                COALESCE((SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.variant.product = p), 0L),
                (SELECT MAX(pp.discountPercentage)
                 FROM PromotionProduct pp JOIN pp.promotion pr
                 WHERE pp.product = p
                   AND pr.status = 1
                   AND pr.promotionType = 'PRODUCT'
                   AND pr.startDate <= CURRENT_TIMESTAMP
                   AND pr.endDate >= CURRENT_TIMESTAMP),
                COALESCE((SELECT AVG(r.rating) FROM Review r WHERE r.product = p), 0.0),
                COALESCE((SELECT COUNT(r) FROM Review r WHERE r.product = p), 0L),
                COALESCE((SELECT COUNT(f) FROM Favorite f WHERE f.product = p), 0L)
            )
            FROM Product p
            WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND p.status = 1
              AND (:shopId = 0 OR p.shop.shopId = :shopId)
            """,
            countQuery = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.status = 1 AND (:shopId = 0 OR p.shop.shopId = :shopId)")
    Page<ProductSaleDTO> findProductSaleDataByKeyword(@Param("keyword") String keyword,
            @Param("shopId") Integer shopId, Pageable pageable);

    @Query("""
            SELECT new com.alotra.model.ProductSaleDTO(
                p,
                COALESCE((SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.variant.product = p), 0L),
                (SELECT MAX(pp.discountPercentage)
                 FROM PromotionProduct pp JOIN pp.promotion pr
                 WHERE pp.product = p
                   AND pr.status = 1
                   AND pr.promotionType = 'PRODUCT'
                   AND pr.startDate <= CURRENT_TIMESTAMP
                   AND pr.endDate >= CURRENT_TIMESTAMP),
                COALESCE((SELECT AVG(r.rating) FROM Review r WHERE r.product = p), 0.0),
                COALESCE((SELECT COUNT(r) FROM Review r WHERE r.product = p), 0L),
                COALESCE((SELECT COUNT(f) FROM Favorite f WHERE f.product = p), 0L)
            )
            FROM Product p
            WHERE p.shop.shopId = :shopId AND p.status = 1
            ORDER BY p.productID DESC
            """)
    Page<ProductSaleDTO> findActiveProductsByShop(@Param("shopId") Integer shopId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.status = 1 ORDER BY p.productName")
    List<Product> findActiveProductsByShop(@Param("shopId") Integer shopId);

    Page<Product> findByStatus(Byte status, Pageable pageable);

    @Query("SELECT p.productID FROM Product p WHERE p.shop.shopId IN :shopIds")
    List<Integer> findProductIdsByShopIds(List<Integer> shopIds);
}
