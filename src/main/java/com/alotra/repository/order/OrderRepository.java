package com.alotra.repository.order;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.order.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    Page<Order> findByShop_ShopIdOrderByOrderDateDesc(Integer shopId, Pageable pageable);
    
    @Query("SELECT o FROM Order o " +
           "WHERE o.shop.shopId = :shopId " +
           "AND (:status IS NULL OR o.orderStatus = :status) " +
           "ORDER BY o.orderDate DESC")
    Page<Order> findShopOrdersByStatus(
        @Param("shopId") Integer shopId,
        @Param("status") String status,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.shop.shopId = :shopId AND o.orderStatus = :status")
    Long countByShopIdAndStatus(@Param("shopId") Integer shopId, @Param("status") String status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.shop.shopId = :shopId")
    Long countByShopId(@Param("shopId") Integer shopId);
    
    Page<Order> findByUser_UserIDOrderByOrderDateDesc(Integer userId, Pageable pageable);
    
    @Query("SELECT o FROM Order o " +
           "WHERE o.user.userID = :userId " +
           "AND (:status IS NULL OR o.orderStatus = :status) " +
           "ORDER BY o.orderDate DESC")
    Page<Order> findUserOrdersByStatus(
        @Param("userId") Integer userId,
        @Param("status") String status,
        Pageable pageable
    );
    
    @Query("SELECT o FROM Order o " +
           "WHERE o.shipper.userID = :shipperId " +
           "AND (:status IS NULL OR o.orderStatus = :status) " +
           "ORDER BY CASE WHEN o.orderStatus = 'Delivering' THEN 1 ELSE 2 END, o.orderDate DESC")
    List<Order> findShipperOrders(
        @Param("shipperId") Integer shipperId,
        @Param("status") String status
    );
}
