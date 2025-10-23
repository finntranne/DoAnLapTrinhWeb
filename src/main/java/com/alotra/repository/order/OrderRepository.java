package com.alotra.repository.order;


import com.alotra.entity.order.Order;
import com.alotra.entity.user.Customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
	
	List<Order> findByCustomerOrderByOrderDateDesc(Customer customer);
}
