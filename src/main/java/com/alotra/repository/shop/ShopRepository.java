package com.alotra.repository.shop;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.shop.Shop;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    
    Optional<Shop> findByUser_UserID(Integer userId);
    
    Optional<Shop> findByShopName(String shopName);
    
    List<Shop> findByStatus(Byte status);
    
    @Query("SELECT s FROM Shop s WHERE s.status = 1 AND " +
           "(SELECT SUM(p.soldCount) FROM Product p WHERE p.shop = s) > :minSales")
    List<Shop> findActiveShopsWithMinSales(@Param("minSales") Integer minSales);
    
    @Query("SELECT COUNT(s) FROM Shop s WHERE s.status = :status")
    Long countByStatus(@Param("status") Byte status);
}
