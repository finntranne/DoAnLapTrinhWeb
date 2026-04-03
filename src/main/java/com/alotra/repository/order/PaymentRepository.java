package com.alotra.repository.order;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.order.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByOrder_OrderID(Integer orderId);
}
