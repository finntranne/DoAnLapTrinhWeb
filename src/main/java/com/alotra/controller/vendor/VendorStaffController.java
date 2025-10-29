package com.alotra.controller.vendor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.dto.shop.EmployeeSearchRequest;
import com.alotra.dto.shop.ShopEmployeeDTO;
import com.alotra.entity.user.User;
import com.alotra.repository.shop.ShopEmployeeRepository;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorStaffService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j

public class VendorStaffController {

	private final VendorStaffService vendorStaffService;
//	private final VendorShopService vendorStaffService;
	private final ShopEmployeeRepository shopEmployeeRepository;

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

	// ==================== STAFF MANAGEMENT ====================
	@GetMapping("/staff")
	public String listStaff(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) String status, @RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			Page<ShopEmployeeDTO> employees = vendorStaffService.getShopEmployees(shopId, status, search, pageable);

			model.addAttribute("employees", employees);
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", employees.getTotalPages());
			model.addAttribute("status", status);
			model.addAttribute("search", search);

			return "vendor/staff/list";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading staff list", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải danh sách nhân viên.");
			return "redirect:/vendor/dashboard";
		}
	}

	@GetMapping("/staff/search")
	public String showSearchEmployeeForm(Model model) {
		model.addAttribute("searchRequest", new EmployeeSearchRequest());
		return "vendor/staff/search";
	}

	@PostMapping("/staff/search")
	@ResponseBody
	public ResponseEntity<?> searchUser(@AuthenticationPrincipal MyUserDetails userDetails,
			@Valid @RequestBody EmployeeSearchRequest request) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			User user = vendorStaffService.searchUserForEmployee(request.getSearchTerm());

			// Kiểm tra user đã là employee của shop chưa
			if (shopEmployeeRepository.existsByShop_ShopIdAndUser_Id(shopId, user.getId())) {
				return ResponseEntity.badRequest().body(Map.of("error", "Người dùng này đã là nhân viên của cửa hàng"));
			}

			// Trả về thông tin user
			Map<String, Object> response = new HashMap<>();
			response.put("userId", user.getId());
			response.put("fullName", user.getFullName());
			response.put("email", user.getEmail());
			response.put("phoneNumber", user.getPhoneNumber());
			response.put("avatarURL", user.getAvatarURL());

			return ResponseEntity.ok(response);

		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/staff/add")
	public String addEmployee(@AuthenticationPrincipal MyUserDetails userDetails, @RequestParam Integer userId,
			@RequestParam Integer roleId, RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			vendorStaffService.addEmployee(shopId, userId, roleId);

			redirectAttributes.addFlashAttribute("success", "Thêm nhân viên thành công!");

			return "redirect:/vendor/staff";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error adding employee", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
			return "redirect:/vendor/staff/search";
		}
	}

	@GetMapping("/staff/edit/{id}")
	public String showEditEmployeeForm(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			Model model, RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			ShopEmployeeDTO employee = vendorStaffService.getEmployeeDetail(shopId, id);

			model.addAttribute("employee", employee);

			return "vendor/staff/edit";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading employee for edit", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/vendor/staff";
		}
	}

	@PostMapping("/staff/edit/{id}")
	public String updateEmployee(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			@RequestParam Integer roleId, @RequestParam String status, RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			vendorStaffService.updateEmployee(shopId, id, roleId, status);

			redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin nhân viên thành công!");

			return "redirect:/vendor/staff";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error updating employee", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
			return "redirect:/vendor/staff/edit/" + id;
		}
	}

	@PostMapping("/staff/deactivate/{id}")
	public String deactivateEmployee(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			vendorStaffService.deactivateEmployee(shopId, id);

			redirectAttributes.addFlashAttribute("success", "Đã xóa nhân viên khỏi cửa hàng!");

			return "redirect:/vendor/staff";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error deactivating employee", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
			return "redirect:/vendor/staff";
		}
	}

}
