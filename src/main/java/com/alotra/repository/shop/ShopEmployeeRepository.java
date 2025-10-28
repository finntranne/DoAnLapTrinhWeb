package com.alotra.repository.shop;

import com.alotra.entity.shop.ShopEmployee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopEmployeeRepository extends JpaRepository<ShopEmployee, Integer> {
    
    // Tìm employee theo shop và user
    Optional<ShopEmployee> findByShop_ShopIdAndUser_Id(Integer shopId, Integer userId);
    
    // Kiểm tra user đã là employee của shop chưa
    boolean existsByShop_ShopIdAndUser_Id(Integer shopId, Integer userId);
    
    // Lấy danh sách employee của shop với filter
    @Query("SELECT se FROM ShopEmployee se " +
           "WHERE se.shop.shopId = :shopId " +
           "AND (:status IS NULL OR se.status = :status) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(se.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(se.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(se.user.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ShopEmployee> findShopEmployeesFiltered(
            @Param("shopId") Integer shopId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
    
    // Đếm số employee active của shop
    Long countByShop_ShopIdAndStatus(Integer shopId, String status);
    
    // Lấy tất cả employee active của shop
    List<ShopEmployee> findByShop_ShopIdAndStatus(Integer shopId, String status);
}