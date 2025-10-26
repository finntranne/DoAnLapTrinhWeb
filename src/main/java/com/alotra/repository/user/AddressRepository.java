////package com.alotra.repository.user; // Hoặc package repository của bạn
////
////import com.alotra.entity.user.Address;
////import com.alotra.entity.user.Customer;
////
////import org.springframework.data.jpa.repository.JpaRepository;
////import java.util.List;
////
////public interface AddressRepository extends JpaRepository<Address, Integer> {
////    
////    // Tìm tất cả địa chỉ của một khách hàng
////    List<Address> findByCustomer(Customer customer);
////}
//
//package com.alotra.repository.user; // Giữ package này
//
//import com.alotra.entity.user.Address;
//import com.alotra.entity.user.User; // *** SỬA: Dùng User thay vì Customer ***
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository; // Thêm @Repository
//import java.util.List;
//import java.util.Optional; // Import Optional
//
//@Repository // Thêm annotation
//public interface AddressRepository extends JpaRepository<Address, Integer> { // Khớp Entity
//
//    // *** SỬA: Tìm theo User thay vì Customer ***
//    List<Address> findByUser(User user);
//
//    // *** THÊM: Tìm theo User ID (thường hữu ích hơn) ***
//    List<Address> findByUserId(Integer userId);
//
//    // *** THÊM: Tìm địa chỉ mặc định của User ***
//    Optional<Address> findByUserIdAndIsDefaultTrue(Integer userId);
//}