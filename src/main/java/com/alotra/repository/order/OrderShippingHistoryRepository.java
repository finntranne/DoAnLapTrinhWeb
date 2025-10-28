package com.alotra.repository.order;

import com.alotra.entity.order.OrderShippingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderShippingHistoryRepository extends JpaRepository<OrderShippingHistory, Integer> {
    
    // Lấy tất cả lịch sử giao hàng của một đơn
    List<OrderShippingHistory> findByOrder_OrderIDOrderByTimestampDesc(Integer orderId);
    
    // Lấy lịch sử giao hàng của shipper cho một đơn
    List<OrderShippingHistory> findByOrder_OrderIDAndShipper_IdOrderByTimestampDesc(Integer orderId, Integer shipperId);
    
    // Lấy trạng thái mới nhất của đơn hàng
    @Query("SELECT h FROM OrderShippingHistory h WHERE h.order.orderID = :orderId " +
           "ORDER BY h.timestamp DESC LIMIT 1")
    Optional<OrderShippingHistory> findLatestByOrderId(@Param("orderId") Integer orderId);
    
    // Lấy trạng thái mới nhất của đơn hàng với shipper cụ thể
    @Query("SELECT h FROM OrderShippingHistory h WHERE h.order.orderID = :orderId " +
           "AND h.shipper.id = :shipperId ORDER BY h.timestamp DESC LIMIT 1")
    Optional<OrderShippingHistory> findLatestByOrderIdAndShipperId(
            @Param("orderId") Integer orderId, 
            @Param("shipperId") Integer shipperId);
    
    // Đếm số lần giao hàng thất bại của shipper
    Long countByShipper_IdAndStatus(Integer shipperId, String status);
    
    // Lấy tất cả đơn hàng đang được giao bởi shipper
    @Query("SELECT DISTINCT h.order.orderID FROM OrderShippingHistory h " +
           "WHERE h.shipper.id = :shipperId " +
           "AND h.status IN ('Assigned', 'Picking_Up', 'Delivering', 'Delivery_Attempt') " +
           "AND h.timestamp = (SELECT MAX(h2.timestamp) FROM OrderShippingHistory h2 " +
           "                   WHERE h2.order.orderID = h.order.orderID AND h2.shipper.id = :shipperId)")
    List<Integer> findActiveOrderIdsByShipperId(@Param("shipperId") Integer shipperId);
}