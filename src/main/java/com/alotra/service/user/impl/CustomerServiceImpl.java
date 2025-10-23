package com.alotra.service.user.impl;

import com.alotra.entity.user.Customer;
import com.alotra.entity.user.User;
import com.alotra.repository.user.CustomerRepository; // Import CustomerRepository
import com.alotra.service.user.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository; // Tiêm CustomerRepository

    @Override
    public Optional<Customer> findByUser(User user) {
        // Giả sử CustomerRepository có hàm findByUser
        return customerRepository.findByUser(user);
    }

    @Override
    public Customer findByEmail(String email) {

        return customerRepository.findByEmail(email).orElse(null);
        
}
    
    @Override // Add Override annotation
    public Customer save(Customer customer) {
        // You might add validation or other logic here later
        return customerRepository.save(customer);
    }
}