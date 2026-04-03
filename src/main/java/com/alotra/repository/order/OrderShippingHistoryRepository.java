package com.alotra.repository.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.order.OrderShippingHistory;

@Repository
public interface OrderShippingHistoryRepository extends JpaRepository<OrderShippingHistory, Integer> {

    List<OrderShippingHistory> findByOrder_OrderIDOrderByTimestampDesc(Integer orderId);

    List<OrderShippingHistory> findByOrder_OrderIDAndShipper_IdOrderByTimestampDesc(Integer orderId, Integer shipperId);

    Optional<OrderShippingHistory> findFirstByOrder_OrderIDOrderByTimestampDesc(Integer orderId);

    Optional<OrderShippingHistory> findFirstByOrder_OrderIDAndShipper_IdOrderByTimestampDesc(
            Integer orderId,
            Integer shipperId);

    Long countByShipper_IdAndStatus(Integer shipperId, String status);

    @Query("SELECT DISTINCT h.order.orderID FROM OrderShippingHistory h " +
           "WHERE h.shipper.id = :shipperId " +
           "AND h.status IN ('Assigned', 'Picking_Up', 'Delivering', 'Delivery_Attempt') " +
           "AND h.timestamp = (SELECT MAX(h2.timestamp) FROM OrderShippingHistory h2 " +
           "                   WHERE h2.order.orderID = h.order.orderID AND h2.shipper.id = :shipperId)")
    List<Integer> findActiveOrderIdsByShipperId(@Param("shipperId") Integer shipperId);
}
