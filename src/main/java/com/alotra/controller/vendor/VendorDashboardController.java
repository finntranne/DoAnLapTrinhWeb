package com.alotra.controller.vendor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.dto.response.ApprovalResponseDTO;
import com.alotra.dto.shop.ShopDashboardDTO;
import com.alotra.entity.common.MessageEntity;
import com.alotra.security.MyUserDetails;
import com.alotra.service.chat.ChatService;
import com.alotra.service.vendor.VendorApprovalService;
import com.alotra.service.vendor.VendorShopDashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorDashboardController {
	private final VendorShopDashboardService vendorShopService;
	private final VendorApprovalService vendorApprovalService;
	
	@Autowired
	ChatService chatService;

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

	// ==================== DASHBOARD ====================

	
	
	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal MyUserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			ShopDashboardDTO dashboard = vendorShopService.getShopDashboard(shopId);
			model.addAttribute("dashboard", dashboard);

			List<ApprovalResponseDTO> pendingApprovals = vendorApprovalService.getPendingApprovals(shopId, null, null);

			model.addAttribute("pendingApprovals", pendingApprovals);
			
			List<MessageEntity> recentMessages = chatService.findRecentMessagesForShop(shopId, 5);
		    int unreadCount = chatService.countUnreadMessages(shopId);

		    model.addAttribute("recentMessages", recentMessages);
		    model.addAttribute("unreadCount", unreadCount);
		    model.addAttribute("shopId", shopId);

			return "vendor/dashboard";

		} catch (IllegalStateException e) {
			log.error("Dashboard access error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register"; // Redirect to shop registration
		} catch (Exception e) {
			log.error("Error loading dashboard", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải dashboard");
			return "redirect:/";
		}
	}
}
