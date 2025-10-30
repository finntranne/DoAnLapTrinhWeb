package com.alotra.controller.vendor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.dto.promotion.PromotionRequestDTO;
import com.alotra.dto.promotion.PromotionStatisticsDTO;
import com.alotra.entity.promotion.Promotion;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorPromotionService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorPromotionController {
	private final VendorPromotionService vendorPromotionService;

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

	private Integer getUserIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
		if (userDetails == null || userDetails.getUser() == null) {
			throw new IllegalStateException("User is not authenticated");
		}
		return userDetails.getUser().getId();
	}

	@GetMapping("/promotions")
	public String listPromotions(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) Byte status, @RequestParam(required = false) String promotionType,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			Page<PromotionStatisticsDTO> promotions = vendorPromotionService.getShopPromotions(shopId, status,
					promotionType, pageable);

			model.addAttribute("promotions", promotions);
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", promotions.getTotalPages());
			model.addAttribute("status", status);

			return "vendor/promotions/list";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		}
	}

	@GetMapping("/promotions/create")
	public String showCreatePromotionForm(@AuthenticationPrincipal MyUserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			getShopIdOrThrow(userDetails);
			model.addAttribute("promotion", new PromotionRequestDTO());
			model.addAttribute("action", "create");
			return "vendor/promotions/form";
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		}
	}

	@PostMapping("/promotions/create")
	public String createPromotion(@AuthenticationPrincipal MyUserDetails userDetails,
			@Valid @ModelAttribute("promotion") PromotionRequestDTO request, BindingResult result, Model model,
			RedirectAttributes redirectAttributes) {

		log.info("=== CREATE PROMOTION REQUEST ===");
		log.info("UsageLimit from form: {}", request.getUsageLimit());
		log.info("DiscountType: {}", request.getDiscountType());
		log.info("DiscountValue: {}", request.getDiscountValue());

		if (result.hasErrors()) {
			log.error("Validation errors: {}", result.getAllErrors());
			model.addAttribute("action", "create");
			return "vendor/promotions/form";
		}

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			vendorPromotionService.requestPromotionCreation(shopId, request, userId);

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu tạo khuyến mãi đã được gửi. Vui lòng chờ admin phê duyệt.");

			return "redirect:/vendor/promotions";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (IllegalArgumentException e) {
			log.error("Validation error from service: {}", e.getMessage());
			model.addAttribute("error", e.getMessage());
			model.addAttribute("action", "create");
			return "vendor/promotions/form";
		} catch (JsonProcessingException e) {
			log.error("Error creating promotion", e);
			model.addAttribute("error", "Có lỗi xảy ra khi tạo khuyến mãi");
			model.addAttribute("action", "create");
			return "vendor/promotions/form";
		}
	}

	@GetMapping("/promotions/edit/{id}")
	public String showEditPromotionForm(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			Model model, RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Promotion promotion = vendorPromotionService.getPromotionDetail(shopId, id);

			if ("PRODUCT".equals(promotion.getPromotionType())) {
				model.addAttribute("promotion", promotion);
				model.addAttribute("action", "edit");
				model.addAttribute("promotionId", id);
				return "vendor/promotions/form";
			}

			PromotionRequestDTO dto = vendorPromotionService.convertPromotionToDTO(promotion);

			log.info("=== LOAD EDIT FORM ===");
			log.info("Promotion ID: {}", id);
			log.info("Current UsageLimit in DB: {}", promotion.getUsageLimit());
			log.info("DTO UsageLimit: {}", dto.getUsageLimit());

			model.addAttribute("promotion", dto);
			model.addAttribute("action", "edit");
			model.addAttribute("promotionId", id);

			if (dto.getStartDate() != null) {
				model.addAttribute("formattedStartDate", dto.getStartDate().toString().substring(0, 16));
			}
			if (dto.getEndDate() != null) {
				model.addAttribute("formattedEndDate", dto.getEndDate().toString().substring(0, 16));
			}

			return "vendor/promotions/form";
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading promotion for edit", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/vendor/promotions";
		}
	}

	@PostMapping("/promotions/edit/{id}")
	public String updatePromotion(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			@Valid @ModelAttribute("promotion") PromotionRequestDTO request, BindingResult result, Model model,
			RedirectAttributes redirectAttributes) {

		log.info("=== UPDATE PROMOTION REQUEST ===");
		log.info("Promotion ID: {}", id);
		log.info("UsageLimit from form: {}", request.getUsageLimit());
		log.info("DiscountType: {}", request.getDiscountType());
		log.info("DiscountValue: {}", request.getDiscountValue());
		log.info("PromoCode: {}", request.getPromoCode());
		log.info("StartDate: {}", request.getStartDate());
		log.info("EndDate: {}", request.getEndDate());

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Promotion promotion = vendorPromotionService.getPromotionDetail(shopId, id);

			if ("PRODUCT".equals(promotion.getPromotionType())) {
				log.warn("Attempt to edit PRODUCT type promotion");
				redirectAttributes.addFlashAttribute("error",
						"Không thể chỉnh sửa khuyến mãi sản phẩm. Vui lòng sửa ở trang Quản lý sản phẩm.");
				return "redirect:/vendor/promotions";
			}

			if (result.hasErrors()) {
				log.error("Validation errors: {}", result.getAllErrors());
				result.getAllErrors().forEach(error -> {
					log.error("  - {}", error.toString());
				});
				model.addAttribute("action", "edit");
				model.addAttribute("promotionId", id);
				return "vendor/promotions/form";
			}

			Integer userId = getUserIdOrThrow(userDetails);

			log.info("Calling service.requestPromotionUpdate...");
			request.setPromotionId(id);
			vendorPromotionService.requestPromotionUpdate(shopId, request, userId);
			log.info("Service call completed successfully");

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu cập nhật khuyến mãi đã được gửi. Vui lòng chờ admin phê duyệt.");

			return "redirect:/vendor/promotions";

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (IllegalArgumentException e) {
			log.error("Validation error from service: {}", e.getMessage(), e);
			model.addAttribute("error", e.getMessage());
			model.addAttribute("action", "edit");
			model.addAttribute("promotionId", id);

			// Load lại promotion data để form không bị trống
			try {
				Promotion promotion = vendorPromotionService.getPromotionDetail(getShopIdOrThrow(userDetails), id);
				PromotionRequestDTO dto = vendorPromotionService.convertPromotionToDTO(promotion);
				model.addAttribute("promotion", dto);

				if (dto.getStartDate() != null) {
					model.addAttribute("formattedStartDate", dto.getStartDate().toString().substring(0, 16));
				}
				if (dto.getEndDate() != null) {
					model.addAttribute("formattedEndDate", dto.getEndDate().toString().substring(0, 16));
				}
			} catch (Exception ex) {
				log.error("Error reloading promotion data", ex);
			}

			return "vendor/promotions/form";
		} catch (Exception e) {
			log.error("Unexpected error updating promotion", e);

			if (e.getMessage() != null && e.getMessage().contains("đã có yêu cầu đang chờ phê duyệt")) {
				redirectAttributes.addFlashAttribute("warning", e.getMessage());
				return "redirect:/vendor/promotions";
			} else {
				redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
				return "redirect:/vendor/promotions/edit/" + id;
			}
		}
	}

	@PostMapping("/promotions/delete/{id}")
	public String deletePromotion(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			RedirectAttributes redirectAttributes) {

		log.info("=== DELETE PROMOTION REQUEST ===");
		log.info("Promotion ID: {}", id);

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			vendorPromotionService.requestPromotionDeletion(shopId, id, userId);

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu xóa khuyến mãi đã được gửi. Vui lòng chờ admin phê duyệt.");

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		} catch (Exception e) {
			log.error("Error deleting promotion", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}

		return "redirect:/vendor/promotions";
	}
}