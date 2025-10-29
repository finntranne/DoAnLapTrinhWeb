package com.alotra.service.order;

import com.alotra.dto.order.ShipperOrderDTO;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderDetail;
import com.alotra.entity.order.OrderHistory;
import com.alotra.entity.order.OrderShippingHistory;
import com.alotra.entity.shop.ShopEmployee;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderHistoryRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.OrderShippingHistoryRepository;
import com.alotra.repository.shop.ShopEmployeeRepository;
import com.alotra.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipperOrderService {

	private final OrderRepository orderRepository;
	private final OrderShippingHistoryRepository shippingHistoryRepository;
	private final OrderHistoryRepository orderHistoryRepository;
	private final ShopEmployeeRepository shopEmployeeRepository;
	private final UserRepository userRepository;

	/**
	 * Lấy danh sách đơn hàng của shipper
	 */
	public Page<ShipperOrderDTO> getShipperOrders(Integer shipperId, String status, String search, Pageable pageable) {
		Page<Order> orders = orderRepository.findShipperOrdersFiltered(shipperId, status, search, pageable);

		List<ShipperOrderDTO> dtos = orders.getContent().stream().map(this::convertToDTO).collect(Collectors.toList());

		return new PageImpl<>(dtos, pageable, orders.getTotalElements());
	}

	/**
	 * Lấy chi tiết đơn hàng của shipper
	 */
	public Optional<ShipperOrderDTO> getOrderDetail(Integer orderId, Integer shipperId) {
		Optional<Order> orderOpt = orderRepository.findById(orderId);

		if (orderOpt.isEmpty()) {
			return Optional.empty();
		}

		Order order = orderOpt.get();

		// Kiểm tra xem đơn hàng có thuộc về shipper này không
		if (order.getShipper() == null || !order.getShipper().getId().equals(shipperId)) {
			return Optional.empty();
		}

		return Optional.of(convertToDTO(order));
	}

	/**
	 * Cập nhật trạng thái giao hàng
	 */
	@Transactional
	public void updateShippingStatus(Integer orderId, Integer shipperId, String newStatus, String notes,
			String imageURL) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

		// Kiểm tra shipper có phải là người được giao đơn không
		if (order.getShipper() == null || !order.getShipper().getId().equals(shipperId)) {
			throw new RuntimeException("Bạn không có quyền cập nhật đơn hàng này");
		}

		// Lấy trạng thái hiện tại
		Optional<OrderShippingHistory> currentHistoryOpt = shippingHistoryRepository
				.findLatestByOrderIdAndShipperId(orderId, shipperId);

		// Kiểm tra trạng thái hợp lệ
		if ("Failed_Delivery".equals(newStatus)) {
			if (currentHistoryOpt.isEmpty()) {
				throw new RuntimeException("Không tìm thấy lịch sử giao hàng");
			}

			String currentStatus = currentHistoryOpt.get().getStatus();
			if (!"Assigned".equals(currentStatus) && !"Picking_Up".equals(currentStatus)) {
				throw new RuntimeException(
						"Chỉ có thể hủy đơn khi đang ở trạng thái 'Đã nhận đơn' hoặc 'Đang lấy hàng'");
			}
		}

		// Tạo lịch sử giao hàng mới
		OrderShippingHistory history = new OrderShippingHistory();
		history.setOrder(order);
		history.setShipper(order.getShipper());
		history.setStatus(newStatus);
		history.setNotes(notes);
		history.setImageURL(imageURL);
		history.setTimestamp(LocalDateTime.now());
		shippingHistoryRepository.save(history);

		// Cập nhật trạng thái đơn hàng và thanh toán nếu giao thành công
		if ("Delivered".equals(newStatus)) {
			order.setOrderStatus("Completed");
			order.setCompletedAt(LocalDateTime.now());

			// Nếu thanh toán khi nhận hàng (COD), cập nhật trạng thái thanh toán
			if ("COD".equalsIgnoreCase(order.getPaymentMethod()) || "Cash".equalsIgnoreCase(order.getPaymentMethod())) {
				order.setPaymentStatus("Paid");
				order.setPaidAt(LocalDateTime.now());
			}

			orderRepository.save(order);

			// Lưu lịch sử đơn hàng
			saveOrderHistory(order, "Delivering", "Completed", shipperId, "Đơn hàng đã được giao thành công");
		}

		// Nếu ghi nhận lần thử giao, cập nhật trạng thái đơn hàng
		if ("Delivery_Attempt".equals(newStatus)) {
			order.setOrderStatus("Delivering");
			orderRepository.save(order);

			// Lưu lịch sử đơn hàng
			saveOrderHistory(order, "Delivering", "Delivering", shipperId,
					"Đã thử giao hàng nhưng không thành công. Lý do: " + notes);
		}

		// Cập nhật trạng thái đơn hàng khi bắt đầu giao
		if ("Delivering".equals(newStatus)) {
			order.setOrderStatus("Delivering");
			orderRepository.save(order);

			saveOrderHistory(order, order.getOrderStatus(), "Delivering", shipperId, "Đang giao hàng");
		}

		// Nếu giao hàng thất bại, tìm shipper khác
		if ("Failed_Delivery".equals(newStatus)) {
			reassignToAnotherShipper(order, shipperId, notes);
		}
	}

	/**
	 * Gán lại đơn hàng cho shipper khác
	 */
	@Transactional
	protected void reassignToAnotherShipper(Order order, Integer failedShipperId, String reason) {
		// Tìm các shipper khác trong cùng shop
		List<ShopEmployee> activeEmployees = shopEmployeeRepository
				.findByShop_ShopIdAndStatus(order.getShop().getShopId(), "Active");

		// Lọc ra các shipper (không phải shipper hiện tại)
		Optional<User> newShipper = activeEmployees.stream().map(ShopEmployee::getUser)
				.filter(user -> !user.getId().equals(failedShipperId))
				.filter(user -> user.getRoles().stream().anyMatch(role -> "SHIPPER".equals(role.getRoleName())))
				.findFirst();

		if (newShipper.isPresent()) {
			// Gán cho shipper mới
			order.setShipper(newShipper.get());
			orderRepository.save(order);

			// Tạo lịch sử giao hàng mới cho shipper mới
			OrderShippingHistory newHistory = new OrderShippingHistory();
			newHistory.setOrder(order);
			newHistory.setShipper(newShipper.get());
			newHistory.setStatus("Assigned");
			newHistory.setNotes("Được gán lại sau khi shipper trước hủy đơn. Lý do: " + reason);
			newHistory.setTimestamp(LocalDateTime.now());
			shippingHistoryRepository.save(newHistory);

			// Lưu lịch sử đơn hàng
			saveOrderHistory(order, order.getOrderStatus(), order.getOrderStatus(), failedShipperId,
					"Gán lại cho shipper mới: " + newShipper.get().getFullName());
		} else {
			// Không tìm thấy shipper khác, cập nhật trạng thái đơn hàng
			order.setOrderStatus("Pending");
			order.setShipper(null);
			orderRepository.save(order);

			saveOrderHistory(order, "Delivering", "Pending", failedShipperId,
					"Không tìm thấy shipper khác. Lý do hủy: " + reason);
		}
	}

	/**
	 * Lưu lịch sử thay đổi đơn hàng
	 */
	private void saveOrderHistory(Order order, String oldStatus, String newStatus, Integer userId, String notes) {
		OrderHistory history = new OrderHistory();
		history.setOrder(order);
		history.setOldStatus(oldStatus);
		history.setNewStatus(newStatus);
		history.setChangedByUser(userRepository.findById(userId).orElse(null));
		history.setNotes(notes);
		history.setTimestamp(LocalDateTime.now());
		orderHistoryRepository.save(history);
	}

	/**
	 * Lấy lịch sử giao hàng của đơn
	 */
	public List<OrderShippingHistory> getShippingHistory(Integer orderId, Integer shipperId) {
		return shippingHistoryRepository.findByOrder_OrderIDAndShipper_IdOrderByTimestampDesc(orderId, shipperId);
	}

	/**
	 * Chuyển đổi Order sang DTO
	 */
	private ShipperOrderDTO convertToDTO(Order order) {
		ShipperOrderDTO dto = new ShipperOrderDTO();
		dto.setOrderId(order.getOrderID());
		dto.setOrderDate(order.getOrderDate());
		dto.setCustomerName(order.getUser().getFullName());
		dto.setCustomerPhone(order.getUser().getPhoneNumber());
		dto.setRecipientName(order.getRecipientName());
		dto.setRecipientPhone(order.getRecipientPhone());
		dto.setShippingAddress(order.getShippingAddress());
		dto.setGrandTotal(order.getGrandTotal());
		dto.setPaymentMethod(order.getPaymentMethod());
		dto.setPaymentStatus(order.getPaymentStatus());
		dto.setOrderStatus(order.getOrderStatus());
		dto.setNotes(order.getNotes());

		// Thông tin shop
		if (order.getShop() != null) {
			dto.setShopName(order.getShop().getShopName());
			dto.setShopPhone(order.getShop().getPhoneNumber());
			dto.setShopAddress(order.getShop().getAddress());
		}

		// Tính tổng số sản phẩm
		int totalItems = order.getOrderDetails().stream().mapToInt(OrderDetail::getQuantity).sum();
		dto.setTotalItems(totalItems);

		// Lấy trạng thái giao hàng hiện tại
		if (order.getShipper() != null) {
			Optional<OrderShippingHistory> latestHistory = shippingHistoryRepository
					.findLatestByOrderIdAndShipperId(order.getOrderID(), order.getShipper().getId());

			if (latestHistory.isPresent()) {
				dto.setCurrentShippingStatus(latestHistory.get().getStatus());
				dto.setAssignedAt(latestHistory.get().getTimestamp());
				dto.setLastUpdateTime(latestHistory.get().getTimestamp());
			}
		}

		return dto;
	}

	/**
	 * Đếm số đơn hàng theo trạng thái
	 */
	public long countOrdersByStatus(Integer shipperId, String status) {
		if (status == null || status.isEmpty()) {
			return orderRepository.countByShipper_Id(shipperId);
		}
		return orderRepository.countByShipper_IdAndOrderStatus(shipperId, status);
	}

	/**
	 * Tự động tạo lịch sử giao hàng khi vendor gán shipper Phương thức này nên được
	 * gọi từ VendorService khi gán shipper cho đơn
	 */
	@Transactional
	public void createInitialShippingHistory(Integer orderId, Integer shipperId, String notes) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

		User shipper = userRepository.findById(shipperId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy shipper"));

		// Tạo lịch sử giao hàng ban đầu với trạng thái Assigned
		OrderShippingHistory history = new OrderShippingHistory();
		history.setOrder(order);
		history.setShipper(shipper);
		history.setStatus("Assigned");
		history.setNotes(notes != null ? notes : "Đơn hàng đã được gán cho shipper");
		history.setTimestamp(LocalDateTime.now());
		shippingHistoryRepository.save(history);
	}
}