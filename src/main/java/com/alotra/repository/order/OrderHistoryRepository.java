package com.alotra.repository.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.order.OrderHistory;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Integer> {
    
    List<OrderHistory> findByOrder_OrderIDOrderByTimestampDesc(Integer orderId);
    
    @Query("SELECT oh FROM OrderHistory oh " +
           "WHERE oh.order.orderID = :orderId " +
           "ORDER BY oh.timestamp ASC")
    List<OrderHistory> findOrderHistoryTimeline(@Param("orderId") Integer orderId);
    
    // Lấy lịch sử thay đổi của đơn hàng bởi user cụ thể
    List<OrderHistory> findByOrder_OrderIDAndChangedByUser_IdOrderByTimestampDesc(
            Integer orderId, Integer userId);
}