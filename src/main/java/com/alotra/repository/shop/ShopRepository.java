package com.alotra.repository.shop;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Category;
import com.alotra.entity.shop.Shop;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    
    Optional<Shop> findByUser_Id(Integer userId);
    
    Optional<Shop> findByShopName(String shopName);
    
    List<Shop> findByStatus(Byte status);
    
    boolean existsByShopName(String shopName);
    
    @Query("SELECT s FROM Shop s WHERE s.status = 1 AND " +
           "(SELECT SUM(p.soldCount) FROM Product p WHERE p.shop = s) > :minSales")
    List<Shop> findActiveShopsWithMinSales(@Param("minSales") Integer minSales);
    
    @Query("SELECT COUNT(s) FROM Shop s WHERE s.status = :status")
    Long countByStatus(@Param("status") Byte status);
    
    /**
     * Lấy danh sách tất cả các cửa hàng đang hoạt động (status = 1)
     * Chỉ trả về các đối tượng Shop (JPA sẽ chỉ load ShopId và ShopName nếu cần thiết)
     */
    @Query("SELECT s FROM Shop s WHERE s.status = 1 ORDER BY s.shopName")
    List<Shop> findAllActiveShops();

    /**
     * Lấy tên cửa hàng dựa trên ID
     */
    @Query("SELECT s.shopName FROM Shop s WHERE s.shopId = :shopId")
    Optional<String> findShopNameByShopId(@Param("shopId") Integer shopId);
    
    @Query("""
     	    SELECT s FROM Shop s
     	    WHERE (:shopName IS NULL OR LOWER(s.shopName) LIKE LOWER(CONCAT('%', :shopName, '%')))   
     	    AND (:phoneNumber IS NULL OR LOWER(s.phoneNumber) LIKE LOWER(CONCAT('%', :phoneNumber, '%')))   
     	    AND (:address IS NULL OR LOWER(s.address) LIKE LOWER(CONCAT('%', :address, '%')))   
     	    AND (:status IS NULL OR s.status = :status)    	     
     	""")
     	Page<Shop> searchShops(
     	        @Param("shopName") String shopName,   	    
     	        @Param("phoneNumber") String phoneNumber,   	
     	        @Param("address") String address,   	
     	        @Param("status") Integer status,   	       
     	        Pageable pageable
     	);
    
}
