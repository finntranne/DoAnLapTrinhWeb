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
			@ModelAttribute("product") ProductRequestDTO request, BindingResult result, Model model,
			RedirectAttributes redirectAttributes) {

		// Debug log - xem dữ liệu nhận được
		log.info("=== CREATE PRODUCT REQUEST ===");
		log.info("Product name: {}", request.getProductName());
		log.info("Category ID: {}", request.getCategoryId());
		log.info("Description: {}", request.getDescription());
		log.info("Variants count: {}", request.getVariants() != null ? request.getVariants().size() : "NULL");
		if (request.getVariants() != null) {
			for (int i = 0; i < request.getVariants().size(); i++) {
				var v = request.getVariants().get(i);
				log.info("Variant[{}]: sizeId={}, price={}, stock={}, sku={}", i, v.getSizeId(), v.getPrice(),
						v.getStock(), v.getSku());
			}
		}
		log.info("Images count: {}", request.getImages() != null ? request.getImages().size() : "NULL");
		log.info("Primary image index: {}", request.getPrimaryImageIndex());

		if (result.hasErrors()) {
			log.error("Validation errors: {}", result.getAllErrors());
			model.addAttribute("categories", vendorService.getAllCategories());
			model.addAttribute("sizes", vendorService.getAllSizesSimple());
			model.addAttribute("action", "create");
			return "vendor/products/form";
		}

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			log.info("Creating product request - Shop ID: {}, User ID: {}", shopId, userId);
			log.info("Product name: {}", request.getProductName());
			log.info("Category ID: {}", request.getCategoryId());
			log.info("Variants count: {}", request.getVariants() != null ? request.getVariants().size() : 0);
			log.info("Images count: {}", request.getImages() != null ? request.getImages().size() : 0);

			// Validate variants
			if (request.getVariants() == null || request.getVariants().isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "Sản phẩm phải có ít nhất một biến thể");
				return "redirect:/vendor/products/create";
			}

			// Validate images
			if (request.getImages() == null || request.getImages().isEmpty()
					|| request.getImages().stream().allMatch(file -> file == null || file.isEmpty())) {
				redirectAttributes.addFlashAttribute("error", "Vui lòng upload ít nhất một hình ảnh");
				return "redirect:/vendor/products/create";
			}

			vendorService.requestProductCreation(shopId, request, userId);

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu tạo sản phẩm đã được gửi. Vui lòng chờ admin phê duyệt.");

			return "redirect:/vendor/products";

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error creating product", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
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
			@Valid @ModelAttribute("product") ProductRequestDTO request, BindingResult result, Model model,
			RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			log.error("Validation errors: {}", result.getAllErrors());
			try {
				Integer shopId = getShopIdOrThrow(userDetails);
				Product product = vendorService.getProductDetail(shopId, id);
				model.addAttribute("categories", vendorService.getAllCategories());
				model.addAttribute("sizes", vendorService.getAllSizesSimple());
				model.addAttribute("existingImages", product.getImages());
				model.addAttribute("action", "edit");
				model.addAttribute("productId", id);
			} catch (Exception e) {
				log.error("Error loading form data", e);
			}
			return "vendor/products/form";
		}

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Integer userId = getUserIdOrThrow(userDetails);

			log.info("Updating product request - Product ID: {}, Shop ID: {}, User ID: {}", id, shopId, userId);

			// Validate variants
			if (request.getVariants() == null || request.getVariants().isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "Sản phẩm phải có ít nhất một biến thể");
				return "redirect:/vendor/products/edit/" + id;
			}

			request.setProductId(id);
			vendorService.requestProductUpdate(shopId, request, userId);

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu cập nhật sản phẩm đã được gửi. Vui lòng chờ admin phê duyệt.");

			return "redirect:/vendor/products";

			// ... (try block) ...
		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";

			// Sửa lại khối catch (Exception e)
		} catch (Exception e) {
			log.error("Error updating product", e);

			// 1. Kiểm tra xem có phải lỗi "Đang chờ phê duyệt" không
			if (e.getMessage() != null && e.getMessage().contains("Đã có yêu cầu đang chờ phê duyệt")) {

				// 2. Dùng thông báo "warning" (cảnh báo) thay vì "error" (lỗi)
				redirectAttributes.addFlashAttribute("warning", e.getMessage());

				// 3. Chuyển hướng về trang danh sách sản phẩm (list)
				return "redirect:/vendor/products";

			} else {
				// 4. Đối với tất cả các lỗi khác (lỗi validation, lỗi hệ thống...)
				// thì giữ nguyên hành vi cũ: quay lại form edit
				redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
				return "redirect:/vendor/products/edit/" + id;
			}
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