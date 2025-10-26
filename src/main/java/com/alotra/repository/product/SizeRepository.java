//package com.alotra.repository.product;
//
//<<<<<<< HEAD
//import com.alotra.entity.product.Size;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface SizeRepository extends JpaRepository<Size, Integer> {
//=======
//import java.util.List;
//import java.util.Optional;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import com.alotra.entity.product.Size;
//
//@Repository
//public interface SizeRepository extends JpaRepository<Size, Integer> {
//    
//    Optional<Size> findBySizeName(String sizeName);
//    
//    List<Size> findAllByOrderBySizeNameAsc();
//>>>>>>> lam
//}

package com.alotra.repository.product; // Giữ package này

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Size;

@Repository
public interface SizeRepository extends JpaRepository<Size, Integer> { // Khớp Entity

    // --- Giữ lại các phương thức từ nhánh lam ---
    Optional<Size> findBySizeName(String sizeName);

    List<Size> findAllByOrderBySizeNameAsc(); // Hữu ích để lấy danh sách size đã sắp xếp
}