package com.alotra.repository.order;

import java.time.LocalDateTime;
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

    @Query("SELECT COUNT(o) FROM Order o WHERE o.shop.shopId = :shopId AND o.orderStatus = :status")
    Long countByShopIdAndStatus(@Param("shopId") Integer shopId, @Param("status") String status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.shop.shopId = :shopId")
    Long countByShopId(@Param("shopId") Integer shopId);

    Page<Order> findByUser_IdOrderByOrderDateDesc(Integer userId, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.user.id = :userId
              AND (:status IS NULL OR o.orderStatus = :status)
            ORDER BY o.orderDate DESC
            """)
    Page<Order> findUserOrdersByStatus(@Param("userId") Integer userId, @Param("status") String status,
            Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.shipper.id = :shipperId
              AND (:status IS NULL OR o.orderStatus = :status)
            ORDER BY CASE WHEN o.orderStatus = 'Delivering' THEN 1 ELSE 2 END, o.orderDate DESC
            """)
    List<Order> findShipperOrders(@Param("shipperId") Integer shipperId, @Param("status") String status);

    @Query(value = """
            SELECT o FROM Order o
            JOIN o.user u
            WHERE o.shop.shopId = :shopId
              AND (:status IS NULL OR o.orderStatus = :status)
              AND (
                   :searchQuery IS NULL
                   OR LOWER(TRIM(u.fullName)) LIKE LOWER(CONCAT('%', TRIM(:searchQuery), '%'))
                   OR TRIM(u.phoneNumber) LIKE CONCAT('%', TRIM(:searchQuery), '%')
                   OR CAST(o.orderID AS string) LIKE CONCAT('%', TRIM(:searchQuery), '%')
              )
            """,
            countQuery = """
            SELECT COUNT(o) FROM Order o
            JOIN o.user u
            WHERE o.shop.shopId = :shopId
              AND (:status IS NULL OR o.orderStatus = :status)
              AND (
                   :searchQuery IS NULL
                   OR LOWER(TRIM(u.fullName)) LIKE LOWER(CONCAT('%', TRIM(:searchQuery), '%'))
                   OR TRIM(u.phoneNumber) LIKE CONCAT('%', TRIM(:searchQuery), '%')
                   OR CAST(o.orderID AS string) LIKE CONCAT('%', TRIM(:searchQuery), '%')
              )
            """)
    Page<Order> findShopOrdersFiltered(@Param("shopId") Integer shopId, @Param("status") String status,
            @Param("searchQuery") String searchQuery, Pageable pageable);

    Page<Order> findByShop_ShopId(Integer shopId, Pageable pageable);

    Page<Order> findByShop_ShopIdAndOrderStatus(Integer shopId, String orderStatus, Pageable pageable);

    Page<Order> findByShipper_Id(Integer shipperId, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.shipper.id = :shipperId
              AND o.orderStatus = :orderStatus
            ORDER BY o.orderDate DESC
            """)
    List<Order> findByShipper_IdAndOrderStatus(@Param("shipperId") Integer shipperId,
            @Param("orderStatus") String orderStatus, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.shipper.id = :shipperId
              AND (:status IS NULL OR o.orderStatus = :status)
              AND (
                   :search IS NULL
                   OR :search = ''
                   OR LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.user.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR CAST(o.orderID AS string) LIKE CONCAT('%', :search, '%')
              )
            """)
    Page<Order> findShipperOrdersFiltered(@Param("shipperId") Integer shipperId, @Param("status") String status,
            @Param("search") String search, Pageable pageable);

    Long countByShipper_IdAndOrderStatus(Integer shipperId, String orderStatus);

    Long countByShipper_Id(Integer shipperId);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.variant v
            LEFT JOIN FETCH v.product p
            LEFT JOIN FETCH p.images
            LEFT JOIN FETCH o.address
            WHERE o.user.id = :userId
            """)
    Page<Order> findByUser_Id(@Param("userId") Integer userId, Pageable pageable);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.variant v
            LEFT JOIN FETCH v.product p
            LEFT JOIN FETCH p.images
            LEFT JOIN FETCH o.address
            WHERE o.user.id = :userId
              AND LOWER(o.orderStatus) = LOWER(:status)
            """)
    Page<Order> findByUser_IdAndOrderStatusIgnoreCase(@Param("userId") Integer userId,
            @Param("status") String status, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.orderDate >= :startDate
              AND o.orderDate < :endDate
              AND o.orderStatus = 'Completed'
            """)
    List<Order> findCompletedOrdersInRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT o FROM Order o
            WHERE o.shop.shopId = :shopId
              AND o.orderDate >= :startDate
              AND o.orderDate < :endDate
              AND o.orderStatus = 'Completed'
            """)
    List<Order> findCompletedOrdersByShopInRange(@Param("shopId") Integer shopId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.orderDate >= :startDate
              AND o.orderDate < :endDate
              AND o.orderStatus = 'Completed'
            """)
    Long countOrdersCreatedInTimeRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    Long countByShipper_IdAndOrderStatusAndOrderDateBetween(Integer shipperId, String orderStatus,
            LocalDateTime startDate, LocalDateTime endDate);

    Long countByShipper_IdAndOrderDateBetween(Integer shipperId, LocalDateTime startDate, LocalDateTime endDate);
}
