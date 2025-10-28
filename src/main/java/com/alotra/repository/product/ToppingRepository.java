//<<<<<<< HEAD
//package com.alotra.repository.product; // Hoặc package repository của bạn
//
//import com.alotra.entity.product.Topping;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface ToppingRepository extends JpaRepository<Topping, Integer> {
//    // Kế thừa JpaRepository là đủ, đã có findAllById()
//}
//=======
//package com.alotra.repository.product;
//
//import java.util.List;
//import java.util.Optional;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import com.alotra.entity.product.Topping;
//
//@Repository
//public interface ToppingRepository extends JpaRepository<Topping, Integer> {
//    
//    List<Topping> findByStatus(Byte status);
//    
//    Optional<Topping> findByToppingName(String toppingName);
//    
//    @Query("SELECT t FROM Topping t WHERE t.status = 1 ORDER BY t.toppingName")
//    List<Topping> findAllActiveToppings();
//}
//>>>>>>> lam


package com.alotra.repository.product; // Giữ package này

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Topping;

@Repository
public interface ToppingRepository extends JpaRepository<Topping, Integer> { // Khớp Entity

    // Giữ lại các phương thức từ nhánh lam
    List<Topping> findByStatus(Byte status);

    Optional<Topping> findByToppingName(String toppingName);

    @Query("SELECT t FROM Topping t WHERE t.status = 1 ORDER BY t.toppingName")
    List<Topping> findAllActiveToppings();

    @Query("SELECT t FROM Topping t " +
            "WHERE t.shop.shopId = :shopId " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:search IS NULL OR LOWER(t.toppingName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY t.toppingID DESC")
     Page<Topping> findShopToppingsFiltered(
         @Param("shopId") Integer shopId,
         @Param("status") Byte status,
         @Param("search") String search,
         Pageable pageable
     );
    
    @Query("SELECT t FROM Topping t WHERE t.shop.shopId = :shopId AND t.status = 1 ORDER BY t.toppingName")
    List<Topping> findAllActiveToppingsByShop(@Param("shopId") Integer shopId);
    
    List<Topping> findByStatusAndShopIsNull(Byte status);

    
    List<Topping> findAllByToppingIDIn(List<Integer> ids);

	Page<Topping> findByStatus(Byte status, Pageable pageable);
}