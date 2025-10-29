//package com.alotra.repository.order;
//
//<<<<<<< HEAD
//
//import com.alotra.entity.order.Order;
//import com.alotra.entity.user.Customer;
//
//import java.util.List;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//
//public interface OrderRepository extends JpaRepository<Order, Integer> {
//	
//	List<Order> findByCustomerOrderByOrderDateDesc(Customer customer);
//	
//	List<Order> findByCustomerAndOrderStatusOrderByOrderDateDesc(Customer customer, String status);
//=======
//import java.util.List;
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import com.alotra.entity.order.Order;
//
//@Repository
//public interface OrderRepository extends JpaRepository<Order, Integer> {
//
//	Page<Order> findByShop_ShopIdOrderByOrderDateDesc(Integer shopId, Pageable pageable);
//
//	@Query("SELECT o FROM Order o " + "WHERE o.shop.shopId = :shopId "
//			+ "AND (:status IS NULL OR o.orderStatus = :status) " + "ORDER BY o.orderDate DESC")
//	Page<Order> findShopOrdersByStatus(@Param("shopId") Integer shopId, @Param("status") String status,
//			Pageable pageable);
//
//	@Query("SELECT COUNT(o) FROM Order o " + "WHERE o.shop.shopId = :shopId AND o.orderStatus = :status")
//	Long countByShopIdAndStatus(@Param("shopId") Integer shopId, @Param("status") String status);
//
//	@Query("SELECT COUNT(o) FROM Order o WHERE o.shop.shopId = :shopId")
//	Long countByShopId(@Param("shopId") Integer shopId);
//
//	Page<Order> findByUser_IdOrderByOrderDateDesc(Integer userId, Pageable pageable);
//
//	@Query("""
//			    SELECT o FROM Order o
//			    WHERE o.user.id = :userId
//			    AND (:status IS NULL OR o.orderStatus = :status)
//			    ORDER BY o.orderDate DESC
//			""")
//	Page<Order> findUserOrdersByStatus(@Param("userId") Integer userId, @Param("status") String status,
//			Pageable pageable);
//
//	@Query("""
//			    SELECT o FROM Order o
//			    WHERE o.shipper.id = :shipperId
//			    AND (:status IS NULL OR o.orderStatus = :status)
//			    ORDER BY CASE WHEN o.orderStatus = 'Delivering' THEN 1 ELSE 2 END, o.orderDate DESC
//			""")
//	List<Order> findShipperOrders(@Param("shipperId") Integer shipperId, @Param("status") String status);
//
//	@Query("SELECT o FROM Order o JOIN o.user u WHERE o.shop.shopId = :shopId "
//			+ "AND (:status IS NULL OR o.orderStatus = :status) " + "AND (:searchQuery IS NULL OR "
//			+ "     LOWER(TRIM(u.fullName)) LIKE LOWER(CONCAT('%', TRIM(:searchQuery), '%')) OR " + // Thêm TRIM()
//			"     TRIM(u.phoneNumber) LIKE CONCAT('%', TRIM(:searchQuery), '%') OR " + // Thêm TRIM()
//			"     TRIM(o.recipientPhone) LIKE CONCAT('%', TRIM(:searchQuery), '%') OR " + // Thêm TRIM()
//			"     LOWER(TRIM(o.recipientName)) LIKE LOWER(CONCAT('%', TRIM(:searchQuery), '%')) )" + // Thêm TRIM()
//			"ORDER BY o.orderDate DESC")
//	Page<Order> findShopOrdersFiltered(@Param("shopId") Integer shopId, @Param("status") String status,
//			@Param("searchQuery") String searchQuery, Pageable pageable);
//>>>>>>> lam
//}

package com.alotra.repository.order; // Giữ package này

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.order.Order;
// Bỏ import Customer

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> { // Khớp Entity

	// --- Giữ lại TẤT CẢ phương thức từ nhánh lam ---
	Page<Order> findByShop_ShopIdOrderByOrderDateDesc(Integer shopId, Pageable pageable);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.shop.shopId = :shopId AND o.orderStatus = :status")
	Long countByShopIdAndStatus(@Param("shopId") Integer shopId, @Param("status") String status);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.shop.shopId = :shopId")
	Long countByShopId(@Param("shopId") Integer shopId);

	Page<Order> findByUser_IdOrderByOrderDateDesc(Integer userId, Pageable pageable);

	@Query("""
			    SELECT o FROM Order o
			    WHERE o.user.id = :userId
			    AND (:status IS NULL OR o.orderStatus = :status)
			    ORDER BY o.orderDate DESC
			""")
	Page<Order> findUserOrdersByStatus(@Param("userId") Integer userId, @Param("status") String status,
			Pageable pageable);

	@Query("""
			    SELECT o FROM Order o
			    WHERE o.shipper.id = :shipperId
			    AND (:status IS NULL OR o.orderStatus = :status)
			    ORDER BY CASE WHEN o.orderStatus = 'Delivering' THEN 1 ELSE 2 END, o.orderDate DESC
			""")
	List<Order> findShipperOrders(@Param("shipperId") Integer shipperId, @Param("status") String status);

	// Giữ lại query đã sửa với countQuery tường minh
	@Query(value = "SELECT o FROM Order o JOIN o.user u WHERE o.shop.shopId = :shopId "
			+ "AND (:status IS NULL OR o.orderStatus = :status) " + "AND (:searchQuery IS NULL OR "
			+ "     LOWER(TRIM(u.fullName)) LIKE LOWER(CONCAT('%', TRIM(:searchQuery), '%')) OR "
			+ "     TRIM(u.phoneNumber) LIKE CONCAT('%', TRIM(:searchQuery), '%') OR "
			+ "     TRIM(o.recipientPhone) LIKE CONCAT('%', TRIM(:searchQuery), '%') OR "
			+ "     LOWER(TRIM(o.recipientName)) LIKE LOWER(CONCAT('%', TRIM(:searchQuery), '%')) )", countQuery = "SELECT count(o) FROM Order o JOIN o.user u WHERE o.shop.shopId = :shopId "
					+ "AND (:status IS NULL OR o.orderStatus = :status) " + "AND (:searchQuery IS NULL OR "
					+ "     LOWER(TRIM(u.fullName)) LIKE LOWER(CONCAT('%', TRIM(:searchQuery), '%')) OR "
					+ "     TRIM(u.phoneNumber) LIKE CONCAT('%', TRIM(:searchQuery), '%') OR "
					+ "     TRIM(o.recipientPhone) LIKE CONCAT('%', TRIM(:searchQuery), '%') OR "
					+ "     LOWER(TRIM(o.recipientName)) LIKE LOWER(CONCAT('%', TRIM(:searchQuery), '%')) )")
	Page<Order> findShopOrdersFiltered(@Param("shopId") Integer shopId, @Param("status") String status,
			@Param("searchQuery") String searchQuery, Pageable pageable);

	// Tìm đơn hàng theo shop
	Page<Order> findByShop_ShopId(Integer shopId, Pageable pageable);

	// Tìm đơn hàng theo shop và trạng thái
	Page<Order> findByShop_ShopIdAndOrderStatus(Integer shopId, String orderStatus, Pageable pageable);

	// Tìm đơn hàng theo shipper
	Page<Order> findByShipper_Id(Integer shipperId, Pageable pageable);

	// Tìm đơn hàng theo shipper và trạng thái
//	Page<Order> findByShipper_IdAndOrderStatus(Integer shipperId, String orderStatus, Pageable pageable);

	List<Order> findByShipper_IdAndOrderStatus(Integer shipperId, String orderStatus, Pageable pageable);

	// Tìm đơn hàng của shipper với filter
	@Query("SELECT o FROM Order o WHERE o.shipper.id = :shipperId "
			+ "AND (:status IS NULL OR o.orderStatus = :status) " + "AND (:search IS NULL OR :search = '' OR "
			+ "     LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
			+ "     LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :search, '%')) OR "
			+ "     LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
			+ "     CAST(o.orderID AS string) LIKE CONCAT('%', :search, '%'))")
	Page<Order> findShipperOrdersFiltered(@Param("shipperId") Integer shipperId, @Param("status") String status,
			@Param("search") String search, Pageable pageable);

	// Đếm số đơn hàng theo trạng thái của shipper
	Long countByShipper_IdAndOrderStatus(Integer shipperId, String orderStatus);

//    // Tìm đơn hàng của shop với filter
//    @Query("SELECT o FROM Order o WHERE o.shop.shopId = :shopId " +
//           "AND (:status IS NULL OR o.orderStatus = :status) " +
//           "AND (:search IS NULL OR :search = '' OR " +
//           "     LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
//           "     LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
//           "     LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
//           "     LOWER(o.user.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
//    Page<Order> findShopOrdersFiltered(
//            @Param("shopId") Integer shopId,
//            @Param("status") String status,
//            @Param("search") String search,
//            Pageable pageable);
//}

	Long countByShipper_Id(Integer shipperId);

	// Ví dụ sửa findByUser_Id trong OrderRepository
	@Query("SELECT DISTINCT o FROM Order o " + "LEFT JOIN FETCH o.orderDetails od " + "LEFT JOIN FETCH od.variant v "
			+ "LEFT JOIN FETCH v.product p " + "LEFT JOIN FETCH v.size s " + "LEFT JOIN FETCH p.images pi "
			+ "WHERE o.user.id = :userId")
	Page<Order> findByUser_Id(@Param("userId") Integer userId, Pageable pageable);

	// Tương tự cho findByUser_IdAndOrderStatusIgnoreCase
	@Query("SELECT DISTINCT o FROM Order o " + "LEFT JOIN FETCH o.orderDetails od " + "LEFT JOIN FETCH od.variant v "
			+ "LEFT JOIN FETCH v.product p " + "LEFT JOIN FETCH v.size s " + "LEFT JOIN FETCH p.images pi "
			+ "WHERE o.user.id = :userId AND LOWER(o.orderStatus) = LOWER(:status)")
	Page<Order> findByUser_IdAndOrderStatusIgnoreCase(@Param("userId") Integer userId, @Param("status") String status,
			Pageable pageable);
	// Bỏ các phương thức dùng Customer từ HEAD
	// List<Order> findByCustomerOrderByOrderDateDesc(Customer customer);
	// List<Order> findByCustomerAndOrderStatusOrderByOrderDateDesc(Customer
	// customer, String status);

	// Tinh doanh thu trong thang cua tat ca shop
	@Query("SELECT SUM(o.grandTotal) FROM Order o WHERE o.orderDate >= :startDate AND o.orderDate < :endDate AND o.orderStatus = 'Completed'")
	BigDecimal calculateMonthlyRevenue(@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :startDate AND o.orderDate < :endDate AND o.orderStatus = 'Completed'")
	Long countOrdersCreatedInTimeRange(@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query("SELECT SUM(o.grandTotal * o.shop.commissionRate / 100) FROM Order o "
			+ "WHERE o.orderDate >= :startDate AND o.orderDate < :endDate AND o.orderStatus = 'Completed'")
	BigDecimal calculateMonthlyProfit(@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query("SELECT s.shopName, SUM(o.grandTotal) " + "FROM Order o JOIN o.shop s "
			+ "WHERE o.orderDate >= :startDate AND o.orderDate < :endDate AND o.orderStatus = 'Completed' "
			+ "GROUP BY s.shopName " + "ORDER BY SUM(o.grandTotal) DESC")
	List<Object[]> getShopRankingByRevenueWithoutDTO(@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	// Đếm đơn hoàn thành trong khoảng thời gian
	Long countByShipper_IdAndOrderStatusAndCompletedAtBetween(Integer shipperId, String orderStatus,
			LocalDateTime startDate, LocalDateTime endDate);

	// Đếm đơn trong khoảng thời gian (theo orderDate)
	Long countByShipper_IdAndOrderDateBetween(Integer shipperId, LocalDateTime startDate, LocalDateTime endDate);

}