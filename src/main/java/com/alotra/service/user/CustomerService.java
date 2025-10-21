package com.alotra.service.user; // Hoặc package service của bạn

import com.alotra.entity.user.Customer;
import com.alotra.entity.user.User; // Import User entity
import java.util.Optional;

public interface CustomerService {
    // Hàm cần thiết để sửa lỗi trong CartController
    Optional<Customer> findByUser(User user);

    // Thêm các hàm khác nếu cần (ví dụ: findById, save, ...)
}