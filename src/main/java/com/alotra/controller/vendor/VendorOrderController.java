package com.alotra.controller.vendor;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.dto.shop.ShopEmployeeDTO;
import com.alotra.dto.shop.ShopOrderDTO;
import com.alotra.entity.order.Order;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorOrderController {
	private final VendorOrderService vendorService;

	// ==================== HELPER METHOD ====================

	/**
	 * Lấy shopId từ authenticated user Throw exception nếu user chưa có shop
	 */
	private Integer getShopIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
		if (userDetails == null) {
			throw new IllegalStateException("User is not authenticated");
		}

		Integer shopId = userDetails.getShopId();

		if (shopId == null) {
			throw new IllegalStateException("Bạn chưa đăng ký shop. Vui lòng đăng ký shop trước.");
		}

		return shopId;
	}

	/**
	 * Lấy userId từ authenticated user
	 */
	private Integer getUserIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
		if (userDetails == null || userDetails.getUser() == null) {
			throw new IllegalStateException("User is not authenticated");
		}
		return userDetails.getUser().getId();
	}

	// ==================== ORDER MANAGEMENT ====================

	@GetMapping("/orders")
	public String listOrders(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) String status, @RequestParam(required = false) String searchQuery,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			String effectiveStatus = (status != null && status.isEmpty()) ? null : status;

			Page<ShopOrderDTO> orders = vendorService.getShopOrders(shopId, effectiveStatus, searchQuery, pageable);

			// Lấy số lượng đơn hàng theo trạng thái
			Map<String, Long> statusCounts = vendorService.getOrderStatusCounts(shopId);

			model.addAttribute("orders", orders);
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", orders.getTotalPages());
			model.addAttribute("status", effectiveStatus);
			model.addAttribute("searchQuery", searchQuery);
			model.addAttribute("statusCounts", statusCounts);

			return "vendor/orders/list";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading order list", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải danh sách đơn hàng.");
			return "redirect:/vendor/dashboard";
		}
	}

	@GetMapping("/orders/{id}")
	public String viewOrderDetail(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			Model model, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Order order = vendorService.getOrderDetail(shopId, id);

			model.addAttribute("order", order);

			// Cần tải danh sách shipper nếu đơn hàng 'Đã xác nhận' (để gán)
			// HOẶC 'Đang giao' (để có thể thay đổi)
			if ("Confirmed".equals(order.getOrderStatus()) || "Delivering".equals(order.getOrderStatus())) {
				List<ShopEmployeeDTO> availableShippers = vendorService.getAvailableShippers(shopId);
				model.addAttribute("availableShippers", availableShippers);
			}
			return "vendor/orders/detail";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading order detail", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/vendor/orders";
		}
	}

	@PostMapping("/orders/{id}/status")
	public String updateOrderStatus(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			@RequestParam String newStatus, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			vendorService.updateOrderStatus(shopId, id, newStatus, userId);

			redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái đơn hàng thành công");

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		} catch (Exception e) {
			log.error("Error updating order status", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}

		return "redirect:/vendor/orders/" + id;
	}

	@PostMapping("/orders/{id}/assign-shipper")
	public String assignShipper(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			@RequestParam Integer shipperId, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			vendorService.assignShipperToOrder(shopId, id, shipperId, userId);

			redirectAttributes.addFlashAttribute("success", "Đã gán shipper cho đơn hàng thành công");

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error assigning shipper", e);
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
		}

		return "redirect:/vendor/orders/" + id;
	}

	/**
	 * Thay đổi shipper
	 */
	@PostMapping("/orders/{id}/reassign-shipper")
	public String reassignShipper(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			@RequestParam Integer newShipperId, @RequestParam(required = false) String reason,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			String finalReason = (reason != null && !reason.isEmpty()) ? reason : "Thay đổi shipper";

			vendorService.reassignShipper(shopId, id, newShipperId, userId, finalReason);

			redirectAttributes.addFlashAttribute("success", "Đã thay đổi shipper thành công");

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error reassigning shipper", e);
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
		}

		return "redirect:/vendor/orders/" + id;
	}

}