package com.alotra.controller.shipper;

import com.alotra.dto.shipper.ShipperDashboardDTO;
import com.alotra.dto.shipper.ShipperInfoDTO;
import com.alotra.dto.order.ShipperOrderDTO;
import com.alotra.security.MyUserDetails;
import com.alotra.service.shipper.ShipperDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/shipper")
@PreAuthorize("hasAuthority('SHIPPER')")
@RequiredArgsConstructor
@Slf4j
public class ShipperDashboardController {

	private final ShipperDashboardService dashboardService;

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
	 * Trang dashboard shipper
	 */
	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal MyUserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			Integer shipperId = getUserIdOrThrow(userDetails);

			// Lấy thông tin shipper
			ShipperInfoDTO shipperInfo = dashboardService.getShipperInfo(shipperId);
			model.addAttribute("shipperInfo", shipperInfo);

			// Lấy thống kê
			ShipperDashboardDTO stats = dashboardService.getShipperDashboardStats(shipperId);
			model.addAttribute("stats", stats);

			// Lấy đơn hàng cần xử lý (đã được gán, chưa bắt đầu giao)
			List<ShipperOrderDTO> pendingOrders = dashboardService.getPendingOrders(shipperId, 5);
			model.addAttribute("pendingOrders", pendingOrders);

			// Lấy đơn hàng đang giao
			List<ShipperOrderDTO> deliveringOrders = dashboardService.getDeliveringOrders(shipperId, 5);
			model.addAttribute("deliveringOrders", deliveringOrders);

			return "shipper/dashboard";

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập với tài khoản shipper");
			return "redirect:/login";
		} catch (Exception e) {
			log.error("Error loading shipper dashboard", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải dashboard.");
			return "redirect:/";
		}
	}
}