//package com.alotra.repository.cart; // Hoặc package repository của bạn
//
//<<<<<<< HEAD
//import com.alotra.entity.cart.Cart;
//import com.alotra.entity.user.Customer;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//import java.util.Optional;
//
//@Repository
//public interface CartRepository extends JpaRepository<Cart, Long> {
//    // Tìm giỏ hàng theo khách hàng
//    Optional<Cart> findByCustomer(Customer customer);
//}
//=======
//import java.util.Optional;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import com.alotra.entity.cart.Cart;
//
//@Repository
//public interface CartRepository extends JpaRepository<Cart, Integer> {
//    
//    Optional<Cart> findByUser_Id(Integer userId);
//}
//>>>>>>> lam


package com.alotra.repository.cart; // Giữ package này

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.cart.Cart;
// Bỏ import com.alotra.entity.user.Customer;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> { // Sử dụng Integer ID khớp với Cart entity

    // Tìm giỏ hàng theo User ID (từ nhánh lam, khớp với Cart entity)
    Optional<Cart> findByUser_Id(Integer userId);

    // Bỏ phương thức findByCustomer từ HEAD vì Cart entity dùng User
}