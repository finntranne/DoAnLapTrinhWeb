package com.alotra.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alotra.entity.order.OrderDetailTopping;
import com.alotra.entity.order.OrderDetailToppingId;

public interface OrderDetailToppingRepository 
extends JpaRepository<OrderDetailTopping, OrderDetailToppingId> { }

