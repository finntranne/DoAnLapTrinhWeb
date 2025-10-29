package com.alotra.controller.vendor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.dto.shop.ShopProfileDTO;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorShopProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorShopProfileController {

	private final VendorShopProfileService vendorShopProfileService;

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

	// ==================== SHOP PROFILE MANAGEMENT ====================
	// Thêm vào VendorController.java

	@GetMapping("/shop-profile")
	public String viewShopProfile(@AuthenticationPrincipal MyUserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			ShopProfileDTO shopProfile = vendorShopProfileService.getShopProfile(shopId);

			model.addAttribute("shop", shopProfile);

			return "vendor/shop-profile";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading shop profile", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải thông tin cửa hàng.");
			return "redirect:/vendor/dashboard";
		}
	}

	@PostMapping("/shop-profile/update")
	public String updateShopProfile(@AuthenticationPrincipal MyUserDetails userDetails,
			@Valid @ModelAttribute("shop") ShopProfileDTO request, BindingResult result,
			RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			log.error("Validation errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin.");
			return "redirect:/vendor/shop-profile";
		}

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			vendorShopProfileService.updateShopProfile(shopId, request, userId);

			redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin cửa hàng thành công!");

			return "redirect:/vendor/shop-profile";

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error updating shop profile", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
			return "redirect:/vendor/shop-profile";
		}
	}

}
