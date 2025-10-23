package com.alotra.repository.user; // Hoặc package repository của bạn

import com.alotra.entity.user.Address;
import com.alotra.entity.user.Customer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    
    // Tìm tất cả địa chỉ của một khách hàng
    List<Address> findByCustomer(Customer customer);
}