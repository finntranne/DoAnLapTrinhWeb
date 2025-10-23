package com.alotra.repository.user; // Hoặc package repository

import com.alotra.entity.user.Customer;
import com.alotra.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByUser(User user);
    
    Optional<Customer> findByEmail(String email);
}