package com.alotra.controller.vendor;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.dto.response.ApprovalResponseDTO;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorApprovalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorApprovalController {
	private final VendorApprovalService vendorApprovalService;
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

	// ==================== APPROVAL STATUS ====================

	@GetMapping("/approvals")
	public String viewApprovals(@AuthenticationPrincipal MyUserDetails userDetails,
			// *** ADD FILTER PARAMETERS ***
			@RequestParam(required = false) String entityType, @RequestParam(required = false) String actionType,
			Model model, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			// *** PASS FILTERS TO SERVICE ***
			List<ApprovalResponseDTO> approvals = vendorApprovalService.getPendingApprovals(shopId, entityType, actionType);

			model.addAttribute("approvals", approvals);
			// *** ADD FILTERS BACK TO MODEL ***
			model.addAttribute("entityType", entityType);
			model.addAttribute("actionType", actionType);

			return "vendor/approvals";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading approvals page", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải trang phê duyệt.");
			return "redirect:/vendor/dashboard"; // Redirect to dashboard on general error
		}
	}

}
