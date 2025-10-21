package com.alotra.repository.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.order.OrderDetail;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    
    List<OrderDetail> findByOrder_OrderID(Integer orderId);
    
    @Query("SELECT od FROM OrderDetail od " +
           "WHERE od.order.orderID = :orderId")
    List<OrderDetail> findByOrderIdWithVariant(@Param("orderId") Integer orderId);
    
    @Query("SELECT od FROM OrderDetail od " +
           "JOIN FETCH od.variant v " +
           "JOIN FETCH v.product p " +
           "WHERE od.order.user.id = :userId " +
           "AND od.order.orderStatus = 'Completed' " +
           "AND NOT EXISTS (SELECT 1 FROM Review r WHERE r.orderDetail.orderDetailID = od.orderDetailID)")
    List<OrderDetail> findUnreviewedCompletedOrderDetails(@Param("userId") Integer userId);
}
