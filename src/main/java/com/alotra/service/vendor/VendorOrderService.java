package com.alotra.service.vendor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.shop.ShopEmployeeDTO;
import com.alotra.dto.shop.ShopOrderDTO;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderHistory;
import com.alotra.entity.shop.ShopEmployee;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderHistoryRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.shop.ShopEmployeeRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.notification.NotificationService;
import com.alotra.service.order.ShipperOrderService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorOrderService {

	private final ShopEmployeeRepository shopEmployeeRepository;
	private final OrderRepository orderRepository;
	private final NotificationService notificationService;
	private final UserRepository userRepository;
	private final ShipperOrderService shipperOrderService;
	private final OrderHistoryRepository orderHistoryRepository;

	@PersistenceContext
	private EntityManager entityManager;

	// ==================== ORDER MANAGEMENT ====================
	public Page<ShopOrderDTO> getShopOrders(Integer shopId, String status, String searchQuery, Pageable pageable) {

		Page<Order> orders = orderRepository.findShopOrdersFiltered(shopId, status, searchQuery, pageable);

		return orders.map(order -> {
			ShopOrderDTO dto = new ShopOrderDTO();
			dto.setOrderId(order.getOrderID());
			dto.setOrderDate(order.getOrderDate());
			dto.setOrderStatus(order.getOrderStatus());
			dto.setPaymentMethod(order.getPaymentMethod());
			dto.setPaymentStatus(order.getPaymentStatus());
			dto.setGrandTotal(order.getGrandTotal());
			if (order.getUser() != null) {
				dto.setCustomerName(order.getUser().getFullName());
				dto.setCustomerPhone(order.getUser().getPhoneNumber());
			} else {
				dto.setCustomerName("N/A");
				dto.setCustomerPhone("N/A");
			}
			dto.setRecipientName(order.getRecipientName());
			dto.setRecipientPhone(order.getRecipientPhone());
			dto.setShippingAddress(order.getShippingAddress());

			if (order.getShipper() != null) {
				dto.setShipperName(order.getShipper().getFullName());
			}

			dto.setTotalItems(order.getOrderDetails() != null ? order.getOrderDetails().size() : 0);

			return dto;
		});
	}

	/**
	 * Lấy số lượng đơn hàng theo từng trạng thái
	 */
	public Map<String, Long> getOrderStatusCounts(Integer shopId) {
		Map<String, Long> counts = new HashMap<>();

		// Đếm tổng số đơn hàng
		counts.put("ALL", orderRepository.countByShopId(shopId));

		// Đếm theo từng trạng thái
		counts.put("Pending", orderRepository.countByShopIdAndStatus(shopId, "Pending"));
		counts.put("Confirmed", orderRepository.countByShopIdAndStatus(shopId, "Confirmed"));
		counts.put("Delivering", orderRepository.countByShopIdAndStatus(shopId, "Delivering"));
		counts.put("Completed", orderRepository.countByShopIdAndStatus(shopId, "Completed"));
		counts.put("Cancelled", orderRepository.countByShopIdAndStatus(shopId, "Cancelled"));

		return counts;
	}

	public Order getOrderDetail(Integer shopId, Integer orderId) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

		if (!order.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Order does not belong to this shop");
		}

		return order;
	}

	public void updateOrderStatus(Integer shopId, Integer orderId, String newStatus, Integer userId) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

		if (!order.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Order does not belong to this shop");
		}

		validateOrderStatusTransition(order.getOrderStatus(), newStatus);

		String oldStatus = order.getOrderStatus();
		order.setOrderStatus(newStatus);

		if ("Completed".equals(newStatus)) {
			order.setCompletedAt(LocalDateTime.now());
		}

		orderRepository.save(order);

		notificationService.notifyCustomerAboutOrderStatus(order.getUser().getId(), orderId, newStatus);

		log.info("Order status updated - Order ID: {}, Old Status: {}, New Status: {}", orderId, oldStatus, newStatus);
	}

	private void validateOrderStatusTransition(String currentStatus, String newStatus) {
		Map<String, List<String>> allowedTransitions = new HashMap<>();
		allowedTransitions.put("Pending", Arrays.asList("Confirmed", "Cancelled"));
		allowedTransitions.put("Confirmed", Arrays.asList("Delivering", "Cancelled"));
		allowedTransitions.put("Delivering", Arrays.asList("Completed", "Returned"));

		List<String> allowed = allowedTransitions.get(currentStatus);
		if (allowed == null || !allowed.contains(newStatus)) {
			throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + newStatus);
		}
	}

	@Transactional
	public void assignShipperToOrder(Integer shopId, Integer orderId, Integer shipperId, Integer userId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

		if (!order.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Đơn hàng không thuộc về shop của bạn");
		}

		if (order.getShipper() != null) {
			throw new RuntimeException("Đơn hàng đã được gán cho shipper: " + order.getShipper().getFullName());
		}

		ShopEmployee shipperEmployee = shopEmployeeRepository.findByShop_ShopIdAndUser_Id(shopId, shipperId)
				.orElseThrow(() -> new RuntimeException("Shipper không phải là nhân viên của shop"));

		if (!"Active".equals(shipperEmployee.getStatus())) {
			throw new RuntimeException("Shipper không còn hoạt động");
		}

		boolean isShipper = shipperEmployee.getUser().getRoles().stream()
				.anyMatch(role -> "SHIPPER".equals(role.getRoleName()));

		if (!isShipper) {
			throw new RuntimeException("Nhân viên này không phải là shipper");
		}

		order.setShipper(shipperEmployee.getUser());
		order.setOrderStatus("Delivering");
		orderRepository.save(order);

		shipperOrderService.createInitialShippingHistory(orderId, shipperId,
				"Đơn hàng đã được gán cho shipper: " + shipperEmployee.getUser().getFullName());

		OrderHistory history = new OrderHistory();
		history.setOrder(order);
		history.setChangedByUser(userRepository.findById(userId).orElse(null));
		history.setNotes("Gán shipper: " + shipperEmployee.getUser().getFullName());
		history.setTimestamp(LocalDateTime.now());
		orderHistoryRepository.save(history);

		try {
			notificationService.notifyShipperAboutAssignment(order.getShipper().getId(), orderId,
					order.getShippingAddress());
		} catch (Exception e) {
			log.error("Error sending notification: {}", e.getMessage());
		}
		
		log.info("Assigned shipper {} to order {}", shipperId, orderId);
	}

	@Transactional(readOnly = true)
	public List<ShopEmployeeDTO> getAvailableShippers(Integer shopId) {
		List<ShopEmployee> activeEmployees = shopEmployeeRepository.findByShop_ShopIdAndStatus(shopId, "Active");

		return activeEmployees.stream()
				.filter(emp -> emp.getUser().getRoles().stream().anyMatch(role -> "SHIPPER".equals(role.getRoleName())))
				.map(emp -> {
					ShopEmployeeDTO dto = new ShopEmployeeDTO();
					dto.setEmployeeId(emp.getEmployeeId());
					dto.setUserId(emp.getUser().getId());
					dto.setFullName(emp.getUser().getFullName());
					dto.setEmail(emp.getUser().getEmail());
					dto.setPhoneNumber(emp.getUser().getPhoneNumber());
					dto.setAvatarURL(emp.getUser().getAvatarURL());
					dto.setStatus(emp.getStatus());
					dto.setRoleName("SHIPPER");
					return dto;
				}).collect(Collectors.toList());
	}

	@Transactional
	public void reassignShipper(Integer shopId, Integer orderId, Integer newShipperId, Integer userId, String reason) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

		if (!order.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Đơn hàng không thuộc về shop của bạn");
		}

		if (!"Delivering".equals(order.getOrderStatus())) {
			throw new RuntimeException("Chỉ có thể thay đổi shipper khi đơn hàng đang giao");
		}

		User oldShipper = order.getShipper();
		if (oldShipper == null) {
			throw new RuntimeException("Đơn hàng chưa được gán shipper");
		}

		ShopEmployee newShipperEmployee = shopEmployeeRepository.findByShop_ShopIdAndUser_Id(shopId, newShipperId)
				.orElseThrow(() -> new RuntimeException("Shipper mới không phải là nhân viên của shop"));

		if (!"Active".equals(newShipperEmployee.getStatus())) {
			throw new RuntimeException("Shipper mới không còn hoạt động");
		}

		boolean isShipper = newShipperEmployee.getUser().getRoles().stream()
				.anyMatch(role -> "SHIPPER".equals(role.getRoleName()));

		if (!isShipper) {
			throw new RuntimeException("Nhân viên này không phải là shipper");
		}

		order.setShipper(newShipperEmployee.getUser());
		orderRepository.save(order);

		shipperOrderService.createInitialShippingHistory(orderId, newShipperId,
				"Được gán lại từ shipper " + oldShipper.getFullName() + ". Lý do: " + reason);

		OrderHistory history = new OrderHistory();
		history.setOrder(order);
		history.setOldStatus("Delivering");
		history.setNewStatus("Delivering");
		history.setChangedByUser(userRepository.findById(userId).orElse(null));
		history.setNotes("Thay đổi shipper từ " + oldShipper.getFullName() + " sang "
				+ newShipperEmployee.getUser().getFullName() + ". Lý do: " + reason);
		history.setTimestamp(LocalDateTime.now());
		orderHistoryRepository.save(history);

		try {
			notificationService.notifyShipperAboutAssignment(newShipperEmployee.getUser().getId(), orderId,
					order.getShippingAddress());
		} catch (Exception e) {
			log.error("Error sending notification: {}", e.getMessage());
		}

		log.info("Reassigned order {} from shipper {} to shipper {}", orderId, oldShipper.getId(), newShipperId);
	}

}