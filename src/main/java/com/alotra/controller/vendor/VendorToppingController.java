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

import com.alotra.dto.topping.ToppingRequestDTO;
import com.alotra.dto.topping.ToppingStatisticsDTO;
import com.alotra.entity.product.Topping;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorToppingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorToppingController {
	private final VendorToppingService vendorToppingService;

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

	// ==================== TOPPING MANAGEMENT ====================

	@GetMapping("/toppings")
	public String listToppings(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) Byte status, @RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			Page<ToppingStatisticsDTO> toppings = vendorToppingService.getShopToppings(shopId, status, search, pageable);

			model.addAttribute("toppings", toppings);
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", toppings.getTotalPages());
			model.addAttribute("status", status);
			model.addAttribute("search", search);

			return "vendor/toppings/list"; // *** TẠO FILE HTML MỚI ***
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		}
	}

	@GetMapping("/toppings/create")
	public String showCreateToppingForm(@AuthenticationPrincipal MyUserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			getShopIdOrThrow(userDetails);
			model.addAttribute("topping", new ToppingRequestDTO());
			model.addAttribute("action", "create");
			return "vendor/toppings/form"; // *** TẠO FILE HTML MỚI ***
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		}
	}

	@PostMapping("/toppings/create")
	public String createTopping(@AuthenticationPrincipal MyUserDetails userDetails,
			@Valid @ModelAttribute("topping") ToppingRequestDTO request, BindingResult result,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return "vendor/toppings/form";
		}
		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);
			vendorToppingService.requestToppingCreation(shopId, request, userId);
			redirectAttributes.addFlashAttribute("success", "Yêu cầu tạo topping đã được gửi.");
			return "redirect:/vendor/toppings";
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error creating topping request", e);
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
			return "redirect:/vendor/toppings/create";
		}
	}

	@GetMapping("/toppings/edit/{id}")
	public String showEditToppingForm(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			Model model, RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Topping topping = vendorToppingService.getToppingDetail(shopId, id);
			ToppingRequestDTO dto = vendorToppingService.convertToppingToDTO(topping);

			model.addAttribute("topping", dto);
			model.addAttribute("action", "edit");
			model.addAttribute("toppingId", id);
			return "vendor/toppings/form";
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading topping for edit", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/vendor/toppings";
		}
	}

	@PostMapping("/toppings/edit/{id}")
	public String updateTopping(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			@Valid @ModelAttribute("topping") ToppingRequestDTO request, BindingResult result,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return "vendor/toppings/form";
		}
		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);
			request.setToppingId(id);
			vendorToppingService.requestToppingUpdate(shopId, request, userId);
			redirectAttributes.addFlashAttribute("success", "Yêu cầu cập nhật topping đã được gửi.");
			return "redirect:/vendor/toppings";
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error updating topping request", e);
			if (e.getMessage() != null && e.getMessage().contains("đang chờ phê duyệt")) {
				redirectAttributes.addFlashAttribute("warning", e.getMessage());
				return "redirect:/vendor/toppings";
			} else {
				redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
				return "redirect:/vendor/toppings/edit/" + id;
			}
		}
	}

	@PostMapping("/toppings/delete/{id}")
	public String deleteTopping(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);
			vendorToppingService.requestToppingDeletion(shopId, id, userId);
			redirectAttributes.addFlashAttribute("success", "Yêu cầu xóa topping đã được gửi.");
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		} catch (Exception e) {
			log.error("Error deleting topping request", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/vendor/toppings";
	}

}
