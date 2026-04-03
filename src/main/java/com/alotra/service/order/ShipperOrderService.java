package com.alotra.service.order;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderShippingHistory;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.OrderShippingHistoryRepository;
import com.alotra.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipperOrderService {

    private final OrderRepository orderRepository;
    private final OrderShippingHistoryRepository shippingHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createInitialShippingHistory(Integer orderId, Integer shipperId, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang"));
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay shipper"));

        OrderShippingHistory history = new OrderShippingHistory();
        history.setOrder(order);
        history.setShipper(shipper);
        history.setStatus("Assigned");
        history.setNotes(notes != null ? notes : "Don hang duoc gan cho shipper");
        history.setTimestamp(LocalDateTime.now());
        shippingHistoryRepository.save(history);
    }
}
