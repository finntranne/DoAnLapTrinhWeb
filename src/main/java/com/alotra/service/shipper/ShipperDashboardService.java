package com.alotra.service.shipper;

import com.alotra.dto.shipper.ShipperDashboardDTO;
import com.alotra.dto.shipper.ShipperInfoDTO;
import com.alotra.dto.order.ShipperOrderDTO;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderDetail;
import com.alotra.entity.order.OrderShippingHistory;
import com.alotra.entity.shop.ShopEmployee;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.OrderShippingHistoryRepository;
import com.alotra.repository.shop.ShopEmployeeRepository;
import com.alotra.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipperDashboardService {

	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final OrderShippingHistoryRepository shippingHistoryRepository;
	private final ShopEmployeeRepository shopEmployeeRepository;

	/**
	 * Lấy thông tin shipper
	 */
	@Transactional(readOnly = true)
	public ShipperInfoDTO getShipperInfo(Integer shipperId) {
		User shipper = userRepository.findById(shipperId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy shipper"));

		ShipperInfoDTO dto = new ShipperInfoDTO();
		dto.setUserId(shipper.getId());
		dto.setFullName(shipper.getFullName());
		dto.setEmail(shipper.getEmail());
		dto.setPhoneNumber(shipper.getPhoneNumber());
		dto.setAvatarURL(shipper.getAvatarURL());

		// Lấy thông tin shop mà shipper đang làm việc
		Optional<ShopEmployee> employeeOpt = shopEmployeeRepository.findFirstByUser_IdAndStatus(shipperId, "Active");

		if (employeeOpt.isPresent()) {
			ShopEmployee employee = employeeOpt.get();
			dto.setShopName(employee.getShop().getShopName());
			dto.setShopAddress(employee.getShop().getAddress());
			dto.setShopPhone(employee.getShop().getPhoneNumber());
		}

		return dto;
	}

	/**
	 * Lấy thống kê dashboard
	 */
	@Transactional(readOnly = true)
	public ShipperDashboardDTO getShipperDashboardStats(Integer shipperId) {
		ShipperDashboardDTO stats = new ShipperDashboardDTO();

		// Đơn được gán (Confirmed - chưa có shipping history hoặc status Assigned)
		Long assignedCount = orderRepository.countByShipper_IdAndOrderStatus(shipperId, "Confirmed");
		stats.setAssignedCount(assignedCount);

		// Đơn đang giao (Delivering)
		Long deliveringCount = orderRepository.countByShipper_IdAndOrderStatus(shipperId, "Delivering");
		stats.setDeliveringCount(deliveringCount);

		// Đơn hoàn thành hôm nay
		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		LocalDateTime endOfDay = startOfDay.plusDays(1);
		Long completedTodayCount = orderRepository.countByShipper_IdAndOrderStatusAndCompletedAtBetween(shipperId,
				"Completed", startOfDay, endOfDay);
		stats.setCompletedTodayCount(completedTodayCount);

		// Tổng đơn trong tuần (từ thứ 2 đến hiện tại)
		LocalDateTime startOfWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
		LocalDateTime now = LocalDateTime.now();
		Long weeklyCount = orderRepository.countByShipper_IdAndOrderDateBetween(shipperId, startOfWeek, now);
		stats.setWeeklyCount(weeklyCount);

		// Tổng đơn đã hoàn thành
		Long totalCompletedCount = orderRepository.countByShipper_IdAndOrderStatus(shipperId, "Completed");
		stats.setTotalCompletedCount(totalCompletedCount);

		// Tính tỷ lệ thành công
		Long totalAssignedEver = orderRepository.countByShipper_Id(shipperId);
		Double successRate = 0.0;
		if (totalAssignedEver > 0) {
			successRate = (totalCompletedCount.doubleValue() / totalAssignedEver.doubleValue()) * 100;
			successRate = Math.round(successRate * 100.0) / 100.0; // Làm tròn 2 chữ số
		}
		stats.setSuccessRate(successRate);

		// Số ngày làm việc (từ ngày được gán đơn đầu tiên)
		Optional<ShopEmployee> employeeOpt = shopEmployeeRepository.findFirstByUser_IdAndStatus(shipperId, "Active");

		Long workingDays = 0L;
		if (employeeOpt.isPresent()) {
			LocalDateTime assignedAt = employeeOpt.get().getAssignedAt();
			if (assignedAt != null) {
				workingDays = ChronoUnit.DAYS.between(assignedAt.toLocalDate(), LocalDate.now()) + 1;
			}
		}
		stats.setWorkingDays(workingDays);

		return stats;
	}

	/**
	 * Lấy danh sách đơn hàng cần xử lý (đã được gán)
	 */
	@Transactional(readOnly = true)
	public List<ShipperOrderDTO> getPendingOrders(Integer shipperId, int limit) {
		PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("orderDate").descending());

		List<Order> orders = orderRepository.findByShipper_IdAndOrderStatus(shipperId, "Confirmed", pageRequest);

		return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
	}

	/**
	 * Lấy danh sách đơn hàng đang giao
	 */
	@Transactional(readOnly = true)
	public List<ShipperOrderDTO> getDeliveringOrders(Integer shipperId, int limit) {
		PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("orderDate").descending());

		List<Order> orders = orderRepository.findByShipper_IdAndOrderStatus(shipperId, "Delivering", pageRequest);

		return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
	}

	/**
	 * Convert Order sang ShipperOrderDTO
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
}