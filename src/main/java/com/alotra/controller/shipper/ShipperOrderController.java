package com.alotra.controller.shipper;

import com.alotra.dto.order.ShipperOrderDTO;
import com.alotra.entity.order.OrderShippingHistory;
import com.alotra.security.MyUserDetails;
import com.alotra.service.order.ShipperOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/shipper/orders")
@PreAuthorize("hasAuthority('SHIPPER')")
@RequiredArgsConstructor
@Slf4j
public class ShipperOrderController {

	private final ShipperOrderService shipperOrderService;

	/**
	 * Lấy userId từ authenticated user
	 */
	private Integer getUserIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
		if (userDetails == null || userDetails.getUser() == null) {
			throw new IllegalStateException("User is not authenticated");
		}
		return userDetails.getUser().getId();
	}

	/**
	 * Danh sách đơn hàng của shipper
	 */
	@GetMapping
	public String listOrders(@RequestParam(required = false) String status,
			@RequestParam(required = false) String searchQuery, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @AuthenticationPrincipal MyUserDetails userDetails,
			Model model, RedirectAttributes redirectAttributes) {

		try {
			// Lấy thông tin shipper hiện tại
			Integer shipperId = getUserIdOrThrow(userDetails);

			// Tạo pageable
			Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());

			// Lấy danh sách đơn hàng
			Page<ShipperOrderDTO> orders = shipperOrderService.getShipperOrders(shipperId, status, searchQuery,
					pageable);

			// Đếm số đơn theo trạng thái để hiển thị badge
			long assignedCount = shipperOrderService.countOrdersByStatus(shipperId, "Confirmed");
			long deliveringCount = shipperOrderService.countOrdersByStatus(shipperId, "Delivering");
			long completedCount = shipperOrderService.countOrdersByStatus(shipperId, "Completed");

			model.addAttribute("orders", orders.getContent());
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", orders.getTotalPages());
			model.addAttribute("totalItems", orders.getTotalElements());
			model.addAttribute("status", status);
			model.addAttribute("searchQuery", searchQuery);
			model.addAttribute("assignedCount", assignedCount);
			model.addAttribute("deliveringCount", deliveringCount);
			model.addAttribute("completedCount", completedCount);

			return "shipper/orders/list";

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập với tài khoản shipper");
			return "redirect:/login";
		} catch (Exception e) {
			log.error("Error loading order list", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải danh sách đơn hàng.");
			return "redirect:/";
		}
	}

	/**
	 * Chi tiết đơn hàng
	 */
	@GetMapping("/{id}")
	public String orderDetail(@PathVariable Integer id, @AuthenticationPrincipal MyUserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shipperId = getUserIdOrThrow(userDetails);

			Optional<ShipperOrderDTO> orderOpt = shipperOrderService.getOrderDetail(id, shipperId);

			if (orderOpt.isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng hoặc bạn không có quyền xem");
				return "redirect:/shipper/orders";
			}

			ShipperOrderDTO order = orderOpt.get();

			// Lấy lịch sử giao hàng
			List<OrderShippingHistory> shippingHistory = shipperOrderService.getShippingHistory(id, shipperId);

			model.addAttribute("order", order);
			model.addAttribute("shippingHistory", shippingHistory);

			return "shipper/orders/detail";

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập với tài khoản shipper");
			return "redirect:/login";
		} catch (Exception e) {
			log.error("Error loading order detail", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
			return "redirect:/shipper/orders";
		}
	}

	/**
	 * Cập nhật trạng thái giao hàng
	 */
	@PostMapping("/{id}/update-status")
	public String updateStatus(@PathVariable Integer id, @RequestParam String status,
			@RequestParam(required = false) String notes, @RequestParam(required = false) String imageURL,
			@AuthenticationPrincipal MyUserDetails userDetails, RedirectAttributes redirectAttributes) {

		try {
			Integer shipperId = getUserIdOrThrow(userDetails);

			shipperOrderService.updateShippingStatus(id, shipperId, status, notes, imageURL);

			String successMessage = getSuccessMessage(status);
			redirectAttributes.addFlashAttribute("success", successMessage);

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập với tài khoản shipper");
			return "redirect:/login";
		} catch (Exception e) {
			log.error("Error updating shipping status", e);
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
		}

		return "redirect:/shipper/orders/" + id;
	}

	/**
	 * Lấy thông báo thành công dựa trên trạng thái
	 */
	private String getSuccessMessage(String status) {
		return switch (status) {
		case "Picking_Up" -> "Đã xác nhận đang lấy hàng";
		case "Delivering" -> "Đã xác nhận đang giao hàng";
		case "Delivery_Attempt" -> "Đã ghi nhận lần giao hàng";
		case "Delivered" -> "Đã xác nhận giao hàng thành công";
		case "Failed_Delivery" -> "Đã hủy đơn hàng và tìm shipper khác";
		default -> "Đã cập nhật trạng thái thành công";
		};
	}
}