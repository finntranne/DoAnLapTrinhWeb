//package com.alotra.repository.cart;
//
//<<<<<<< HEAD
//import com.alotra.entity.cart.Cart;
//import com.alotra.entity.cart.CartItem;
//import com.alotra.entity.product.ProductVariant;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//import java.util.Optional;
//
//@Repository
//public interface CartItemRepository extends JpaRepository<CartItem, Long> {
//    // Tìm một món hàng cụ thể trong giỏ theo biến thể (size)
//    // (Chúng ta sẽ kiểm tra topping trong Service)
//    Optional<CartItem> findByCartAndVariant(Cart cart, ProductVariant variant);
//=======
//import java.util.List;
//import java.util.Optional;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import com.alotra.entity.cart.CartItem;
//
//@Repository
//public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
//    
//    List<CartItem> findByCart_CartID(Integer cartId);
//    
//    Optional<CartItem> findByCart_CartIDAndVariant_VariantID(Integer cartId, Integer variantId);
//    
//    void deleteByCart_CartID(Integer cartId);
//    
//    @Query("SELECT ci FROM CartItem ci " +
//           "JOIN FETCH ci.variant v " +
//           "JOIN FETCH v.product p " +
//           "WHERE ci.cart.cartID = :cartId")
//    List<CartItem> findByCartIdWithDetails(@Param("cartId") Integer cartId);
//>>>>>>> lam
//}


package com.alotra.repository.cart; // Giữ package này

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.cart.Cart; // Import Cart
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.product.ProductVariant; // Import ProductVariant

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> { // Sử dụng Integer ID khớp CartItem entity

    // Giữ lại các phương thức từ nhánh lam
    List<CartItem> findByCart_CartID(Integer cartId);

    Optional<CartItem> findByCart_CartIDAndVariant_VariantID(Integer cartId, Integer variantId);

    void deleteByCart_CartID(Integer cartId); // Hữu ích để xóa toàn bộ giỏ hàng

    // Giữ lại query fetch join từ nhánh lam để tối ưu tải dữ liệu
    @Query("SELECT ci FROM CartItem ci " +
           "JOIN FETCH ci.variant v " +
           "JOIN FETCH v.product p " +
           // Optional: JOIN FETCH toppings if always needed, but be mindful of performance
           // "LEFT JOIN FETCH ci.selectedToppings " +
           "WHERE ci.cart.cartID = :cartId")
    List<CartItem> findByCartIdWithDetails(@Param("cartId") Integer cartId);

    // Thêm phương thức findByCartAndVariant từ HEAD (hữu ích trong service)
    // Sử dụng kiểu đối tượng thay vì ID
    Optional<CartItem> findByCartAndVariant(Cart cart, ProductVariant variant);
}