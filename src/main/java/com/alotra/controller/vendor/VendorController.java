package com.alotra.controller.vendor;

import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.dto.product.ProductStatisticsDTO;
import com.alotra.dto.promotion.PromotionRequestDTO;
import com.alotra.dto.response.ApprovalResponseDTO;
import com.alotra.dto.shop.ShopDashboardDTO;
import com.alotra.dto.shop.ShopOrderDTO;
import com.alotra.dto.shop.ShopRevenueDTO;
import com.alotra.entity.order.Order;
import com.alotra.entity.product.Product;
import com.alotra.entity.promotion.Promotion;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorController {

	private final VendorService vendorService;

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

	// ==================== DASHBOARD ====================

	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal MyUserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			ShopDashboardDTO dashboard = vendorService.getShopDashboard(shopId);
			model.addAttribute("dashboard", dashboard);

			// Lấy pending approvals
			List<ApprovalResponseDTO> pendingApprovals = vendorService.getPendingApprovals(shopId);
			model.addAttribute("pendingApprovals", pendingApprovals);

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

	// ==================== PRODUCT MANAGEMENT ====================

	@GetMapping("/products")
	public String listProducts(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) Byte status, @RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			Page<ProductStatisticsDTO> products = vendorService.getShopProducts(shopId, status, search, pageable);

			model.addAttribute("products", products);
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", products.getTotalPages());
			model.addAttribute("status", status);
			model.addAttribute("search", search);

			return "vendor/products/list";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		}
	}

	@GetMapping("/products/create")
	public String showCreateProductForm(@AuthenticationPrincipal MyUserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			getShopIdOrThrow(userDetails);

			model.addAttribute("product", new ProductRequestDTO());
			model.addAttribute("action", "create");

			// Thêm danh sách categories và sizes
			model.addAttribute("categories", vendorService.getAllCategories());
			model.addAttribute("sizes", vendorService.getAllSizesSimple());

			return "vendor/products/form";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		}
	}

	@PostMapping("/products/create")
	public String createProduct(@AuthenticationPrincipal MyUserDetails userDetails,
			@Valid @ModelAttribute("product") ProductRequestDTO request, BindingResult result,
			RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			return "vendor/products/form";
		}

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			vendorService.requestProductCreation(shopId, request, userId);

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu tạo sản phẩm đã được gửi. Vui lòng chờ admin phê duyệt.");

			return "redirect:/vendor/products";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (JsonProcessingException e) {
			log.error("Error creating product", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tạo sản phẩm");
			return "redirect:/vendor/products/create";
		}
	}

	@GetMapping("/products/edit/{id}")
	public String showEditProductForm(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			Model model, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			Product product = vendorService.getProductDetail(shopId, id);
			ProductRequestDTO dto = vendorService.convertProductToDTO(product);

			model.addAttribute("product", dto);
			model.addAttribute("action", "edit");
			model.addAttribute("productId", id);

			model.addAttribute("categories", vendorService.getAllCategories());
			model.addAttribute("sizes", vendorService.getAllSizesSimple());
			model.addAttribute("existingImages", product.getImages());

			return "vendor/products/form";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading product for edit", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/vendor/products";
		}
	}

	@PostMapping("/products/edit/{id}")
	public String updateProduct(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			@Valid @ModelAttribute("product") ProductRequestDTO request, BindingResult result,
			RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			return "vendor/products/form";
		}

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			request.setProductId(id);
			vendorService.requestProductUpdate(shopId, request, userId);

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu cập nhật sản phẩm đã được gửi. Vui lòng chờ admin phê duyệt.");

			return "redirect:/vendor/products";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error updating product", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return ("redirect:/vendor/products/edit/" + id);
		}
	}

	@PostMapping("/products/delete/{id}")
	public String deleteProduct(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			vendorService.requestProductDeletion(shopId, id, userId);

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu xóa sản phẩm đã được gửi. Vui lòng chờ admin phê duyệt.");

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		} catch (Exception e) {
			log.error("Error deleting product", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}

		return "redirect:/vendor/products";
	}

	// ==================== PROMOTION MANAGEMENT ====================

	@GetMapping("/promotions")
	public String listPromotions(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) Byte status, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Model model, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			Page<Promotion> promotions = vendorService.getShopPromotions(shopId, status, pageable);

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
			@Valid @ModelAttribute("promotion") PromotionRequestDTO request, BindingResult result,
			RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			return "vendor/promotions/form";
		}

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			vendorService.requestPromotionCreation(shopId, request, userId);

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu tạo khuyến mãi đã được gửi. Vui lòng chờ admin phê duyệt.");

			return "redirect:/vendor/promotions";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (JsonProcessingException e) {
			log.error("Error creating promotion", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tạo khuyến mãi");
			return "redirect:/vendor/promotions/create";
		}
	}

	@PostMapping("/promotions/delete/{id}")
	public String deletePromotion(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			vendorService.requestPromotionDeletion(shopId, id, userId);

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu xóa khuyến mãi đã được gửi. Vui lòng chờ admin phê duyệt.");

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		} catch (Exception e) {
			log.error("Error deleting promotion", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}

		return "redirect:/vendor/promotions";
	}

	// ==================== ORDER MANAGEMENT ====================

	@GetMapping("/orders")
	public String listOrders(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Model model, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			Page<ShopOrderDTO> orders = vendorService.getShopOrders(shopId, status, pageable);

			model.addAttribute("orders", orders);
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", orders.getTotalPages());
			model.addAttribute("status", status);

			return "vendor/orders/list";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		}
	}

	@GetMapping("/orders/{id}")
	public String viewOrderDetail(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			Model model, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Order order = vendorService.getOrderDetail(shopId, id);

			model.addAttribute("order", order);

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

	// ==================== REVENUE MANAGEMENT ====================

	@GetMapping("/revenue")
	public String viewRevenue(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
			Model model, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			List<ShopRevenueDTO> revenues = vendorService.getShopRevenue(shopId, startDate, endDate);

			model.addAttribute("revenues", revenues);
			model.addAttribute("startDate", startDate);
			model.addAttribute("endDate", endDate);

			return "vendor/revenue";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		}
	}

	// ==================== APPROVAL STATUS ====================

	@GetMapping("/approvals")
	public String viewApprovals(@AuthenticationPrincipal MyUserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			List<ApprovalResponseDTO> approvals = vendorService.getPendingApprovals(shopId);

			model.addAttribute("approvals", approvals);

			return "vendor/approvals";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		}
	}
}