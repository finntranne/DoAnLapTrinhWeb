package com.alotra.repository.shop;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.shop.ShopRevenue;

@Repository
public interface ShopRevenueRepository extends JpaRepository<ShopRevenue, Integer> {
    
    @Query("SELECT sr FROM ShopRevenue sr " +
           "WHERE sr.shop.shopId = :shopId " +
           "AND sr.recordedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY sr.recordedAt DESC")
    List<ShopRevenue> findByShopIdAndDateRange(
        @Param("shopId") Integer shopId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT COALESCE(SUM(sr.netRevenue), 0) FROM ShopRevenue sr " +
           "WHERE sr.shop.shopId = :shopId")
    Double getTotalRevenueByShopId(@Param("shopId") Integer shopId);
    
    @Query("SELECT COALESCE(SUM(sr.netRevenue), 0) FROM ShopRevenue sr " +
           "WHERE sr.shop.shopId = :shopId " +
           "AND sr.recordedAt BETWEEN :startDate AND :endDate")
    Double getRevenueByShopIdAndDateRange(
        @Param("shopId") Integer shopId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT sr.shop.shopId, SUM(sr.netRevenue) " +
           "FROM ShopRevenue sr " +
           "WHERE sr.recordedAt >= :startDate " +
           "GROUP BY sr.shop.shopId " +
           "ORDER BY SUM(sr.netRevenue) DESC")
    List<Object[]> getTopShopsByRevenue(@Param("startDate") LocalDateTime startDate, Pageable pageable);
}
