package com.alotra.controller.vendor;

import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.dto.product.ProductStatisticsDTO;
import com.alotra.dto.promotion.PromotionStatisticsDTO;
import com.alotra.dto.promotion.PromotionRequestDTO;
import com.alotra.dto.response.ApprovalResponseDTO;
import com.alotra.dto.shop.CategoryRevenueDTO;
import com.alotra.dto.shop.ShopDashboardDTO;
import com.alotra.dto.shop.ShopOrderDTO;
import com.alotra.dto.shop.ShopRevenueDTO;
import com.alotra.dto.topping.ToppingRequestDTO;
import com.alotra.dto.topping.ToppingStatisticsDTO;
import com.alotra.entity.order.Order;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.Topping;
import com.alotra.entity.promotion.Promotion;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorController {

	private final VendorService vendorService;
	private final ToppingRepository toppingRepository;
	private final PromotionRepository promotionRepository;
	private final ProductRepository productRepository;

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

			List<ApprovalResponseDTO> pendingApprovals = vendorService.getPendingApprovals(shopId, null, null);

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

//	@GetMapping("/products")
//	public String listProducts(@AuthenticationPrincipal MyUserDetails userDetails,
//			@RequestParam(required = false) Byte status,
//			// *** ADD categoryId PARAMETER ***
//			@RequestParam(required = false) Integer categoryId, @RequestParam(required = false) String search,
//			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size, Model model,
//			RedirectAttributes redirectAttributes) {
//
//		try {
//			Integer shopId = getShopIdOrThrow(userDetails);
//			Pageable pageable = PageRequest.of(page, size);
//
//			// *** PASS categoryId TO SERVICE ***
//			Page<ProductStatisticsDTO> products = vendorService.getShopProducts(shopId, status, categoryId, search,
//					pageable);
//
//			model.addAttribute("products", products);
//			model.addAttribute("currentPage", page);
//			model.addAttribute("totalPages", products.getTotalPages());
//			model.addAttribute("status", status);
//			model.addAttribute("search", search);
//			// *** ADD categoryId BACK TO MODEL ***
//			model.addAttribute("categoryId", categoryId);
//			// *** ADD CATEGORIES LIST TO MODEL ***
//			model.addAttribute("categories", vendorService.getAllCategoriesSimple()); // Use Simple DTO
//
//			return "vendor/products/list";
//
//		} catch (IllegalStateException e) {
//			redirectAttributes.addFlashAttribute("error", e.getMessage());
//			return "redirect:/shop/register";
//		}
//	}

	@GetMapping("/products")
	public String listProducts(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) Byte status, @RequestParam(required = false) Integer categoryId,
			// *** ADD approvalStatus PARAMETER HERE ***
			@RequestParam(required = false) String approvalStatus, @RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page,
			// Note: Size is set to 5 here, was 20 before. Adjust if needed.
			@RequestParam(defaultValue = "5") int size, Model model, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			// *** PASS approvalStatus TO SERVICE CALL HERE ***
			Page<ProductStatisticsDTO> products = vendorService.getShopProducts(shopId, status, categoryId,
					approvalStatus, search, pageable);

			model.addAttribute("products", products);
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", products.getTotalPages());
			model.addAttribute("status", status);
			model.addAttribute("search", search);
			model.addAttribute("categoryId", categoryId);
			// *** ADD approvalStatus TO MODEL HERE ***
			model.addAttribute("approvalStatus", approvalStatus);
			model.addAttribute("categories", vendorService.getAllCategoriesSimple());

			return "vendor/products/list";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) { // Broader catch block is good practice
			log.error("Error loading product list", e); // Use logger if available (@Slf4j)
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải danh sách sản phẩm.");
			return "redirect:/vendor/dashboard";
		}
	}

	@GetMapping("/products/create")
	public String showCreateProductForm(@AuthenticationPrincipal MyUserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			model.addAttribute("product", new ProductRequestDTO());
			model.addAttribute("action", "create");

			// Thêm danh sách categories và sizes
			model.addAttribute("categories", vendorService.getAllCategories());
			model.addAttribute("sizes", vendorService.getAllSizesSimple());
			model.addAttribute("allShopToppings", toppingRepository.findAllActiveToppingsByShop(shopId));
			model.addAttribute("allShopProductPromotions",
					promotionRepository.findAllActiveProductPromotionsByShop(shopId));

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

		log.info("=== CREATE PRODUCT REQUEST ===");
		log.info("Product name: {}", request.getProductName());
		log.info("Discount Percentage: {}", request.getDiscountPercentage()); // *** MỚI ***

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

			// *** BỎ LOGIC TOPPING (nếu không cần) ***
			Set<Topping> selectedToppings = new HashSet<>();
			if (request.getAvailableToppingIds() != null && !request.getAvailableToppingIds().isEmpty()) {
				List<Topping> toppingsFromDb = toppingRepository.findAllById(request.getAvailableToppingIds());
				for (Topping t : toppingsFromDb) {
					if (t.getShop() == null || !t.getShop().getShopId().equals(shopId)) {
						throw new IllegalAccessException("Đã phát hiện topping không hợp lệ.");
					}
				}
				selectedToppings.addAll(toppingsFromDb);
			}

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

			// *** MỚI: Validate Discount Percentage ***
			if (request.getDiscountPercentage() != null) {
				if (request.getDiscountPercentage() < 0 || request.getDiscountPercentage() > 100) {
					redirectAttributes.addFlashAttribute("error", "% Giảm giá phải từ 0-100%");
					return "redirect:/vendor/products/create";
				}
			}

			// *** GỌI SERVICE MỚI (đã bao gồm logic lưu discount) ***
			vendorService.requestProductCreation(shopId, request, userId, selectedToppings);

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

			model.addAttribute("allShopToppings", toppingRepository.findAllActiveToppingsByShop(shopId));

			model.addAttribute("allShopProductPromotions",
					promotionRepository.findAllActiveProductPromotionsByShop(shopId));

			// *** LOG ĐỂ DEBUG ***
			log.info("Loaded product {} for edit with discount: {}%", product.getProductID(),
					dto.getDiscountPercentage());

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

			// Validate variants
			if (request.getVariants() == null || request.getVariants().isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "Sản phẩm phải có ít nhất một biến thể");
				return "redirect:/vendor/products/edit/" + id;
			}

			// *** MỚI: Validate Discount Percentage ***
			if (request.getDiscountPercentage() != null) {
				if (request.getDiscountPercentage() < 0 || request.getDiscountPercentage() > 100) {
					redirectAttributes.addFlashAttribute("error", "% Giảm giá phải từ 0-100%");
					return "redirect:/vendor/products/edit/" + id;
				}
				log.info("Product update includes discount: {}%", request.getDiscountPercentage());
			}

			Set<Topping> selectedToppings = new HashSet<>();
			if (request.getAvailableToppingIds() != null && !request.getAvailableToppingIds().isEmpty()) {
				List<Topping> toppingsFromDb = toppingRepository.findAllById(request.getAvailableToppingIds());
				for (Topping t : toppingsFromDb) {
					if (t.getShop() == null || !t.getShop().getShopId().equals(shopId)) {
						throw new IllegalAccessException("Đã phát hiện topping không hợp lệ.");
					}
				}
				selectedToppings.addAll(toppingsFromDb);
			}

			request.setProductId(id);
			vendorService.requestProductUpdate(shopId, request, userId, selectedToppings);
			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu cập nhật sản phẩm đã được gửi. Vui lòng chờ admin phê duyệt.");

			return "redirect:/vendor/products";

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error updating product", e);
			if (e.getMessage() != null && e.getMessage().contains("đang chờ phê duyệt")) {
				redirectAttributes.addFlashAttribute("warning", e.getMessage());
				return "redirect:/vendor/products";
			} else {
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

	// ==================== TOPPING MANAGEMENT ====================

	@GetMapping("/toppings")
	public String listToppings(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) Byte status, @RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			Page<ToppingStatisticsDTO> toppings = vendorService.getShopToppings(shopId, status, search, pageable);

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
			vendorService.requestToppingCreation(shopId, request, userId);
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
			Topping topping = vendorService.getToppingDetail(shopId, id);
			ToppingRequestDTO dto = vendorService.convertToppingToDTO(topping);

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
			vendorService.requestToppingUpdate(shopId, request, userId);
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
			vendorService.requestToppingDeletion(shopId, id, userId);
			redirectAttributes.addFlashAttribute("success", "Yêu cầu xóa topping đã được gửi.");
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		} catch (Exception e) {
			log.error("Error deleting topping request", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/vendor/toppings";
	}

	// ==================== PROMOTION MANAGEMENT ====================

	@GetMapping("/promotions")
	public String listPromotions(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) Byte status, @RequestParam(required = false) String promotionType,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			Page<PromotionStatisticsDTO> promotions = vendorService.getShopPromotions(shopId, status, promotionType,
					pageable);

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
			// *** BỎ: model.addAttribute("shopProducts", ...) ***
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

	@GetMapping("/promotions/edit/{id}")
	public String showEditPromotionForm(@AuthenticationPrincipal MyUserDetails userDetails, @PathVariable Integer id,
			Model model, RedirectAttributes redirectAttributes) {
		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Promotion promotion = vendorService.getPromotionDetail(shopId, id);

			// *** KIỂM TRA PROMOTION TYPE ***
			if ("PRODUCT".equals(promotion.getPromotionType())) {
				// Nếu là PRODUCT type → Chỉ hiển thị thông tin (read-only)
				model.addAttribute("promotion", promotion);
				model.addAttribute("action", "edit");
				model.addAttribute("promotionId", id);
				return "vendor/promotions/form"; // Trả về cùng view nhưng sẽ hiển thị form khác
			}

			// *** BỎ convertPromotionToDTOWithProducts, CHỈ dùng convertPromotionToDTO ***
			PromotionRequestDTO dto = vendorService.convertPromotionToDTO(promotion);

			model.addAttribute("promotion", dto);
			model.addAttribute("action", "edit");
			model.addAttribute("promotionId", id);

			if (dto.getStartDate() != null) {
				model.addAttribute("formattedStartDate", dto.getStartDate().toString().substring(0, 16));
			}
			if (dto.getEndDate() != null) {
				model.addAttribute("formattedEndDate", dto.getEndDate().toString().substring(0, 16));
			}

			// *** BỎ: model.addAttribute("shopProducts", ...) ***
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

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Promotion promotion = vendorService.getPromotionDetail(shopId, id);

			// *** KIỂM TRA: KHÔNG CHO SỬA PROMOTION PRODUCT TYPE ***
			if ("PRODUCT".equals(promotion.getPromotionType())) {
				redirectAttributes.addFlashAttribute("error",
						"Không thể chỉnh sửa khuyến mãi sản phẩm. Vui lòng sửa ở trang Quản lý sản phẩm.");
				return "redirect:/vendor/promotions";
			}

			// *** CHỈ XỬ LÝ ORDER TYPE ***
			if (result.hasErrors()) {
				log.error("Validation errors: {}", result.getAllErrors());
				model.addAttribute("action", "edit");
				model.addAttribute("promotionId", id);
				return "vendor/promotions/form";
			}

			Integer userId = getUserIdOrThrow(userDetails);

			log.info("Updating promotion request - Promotion ID: {}, Shop ID: {}, User ID: {}", id, shopId, userId);

			request.setPromotionId(id);
			vendorService.requestPromotionUpdate(shopId, request, userId);

			redirectAttributes.addFlashAttribute("success",
					"Yêu cầu cập nhật khuyến mãi đã được gửi. Vui lòng chờ admin phê duyệt.");

			return "redirect:/vendor/promotions";

		} catch (IllegalStateException e) {
			log.error("Auth error: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error updating promotion", e);

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
			@RequestParam(required = false) String status, // Nhận status như cũ
			@RequestParam(required = false) String searchQuery, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size, Model model, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);
			Pageable pageable = PageRequest.of(page, size);

			String effectiveStatus = (status != null && status.isEmpty()) ? null : status;

			Page<ShopOrderDTO> orders = vendorService.getShopOrders(shopId, effectiveStatus, searchQuery, pageable);

			model.addAttribute("orders", orders);
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", orders.getTotalPages());
			model.addAttribute("status", effectiveStatus);
			model.addAttribute("searchQuery", searchQuery);

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

			// Fetch data (defaulting logic is inside service if needed)
			List<ShopRevenueDTO> revenues = vendorService.getShopRevenue(shopId, startDate, endDate);

			model.addAttribute("revenues", revenues);
			List<CategoryRevenueDTO> categoryRevenues = vendorService.getShopRevenueByCategory(shopId, startDate,
					endDate);
			model.addAttribute("categoryRevenues", categoryRevenues);
			// Pass raw dates back for the form
			model.addAttribute("startDate", startDate);
			model.addAttribute("endDate", endDate);

			return "vendor/revenue";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading revenue page", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải trang doanh thu.");
			return "redirect:/vendor/dashboard";
		}
	}

	@GetMapping("/revenue/export")
	public void exportRevenueToExcel(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
			HttpServletResponse response) {

		log.info("Exporting revenue to Excel for shopId: {}, startDate: {}, endDate: {}",
				(userDetails != null ? userDetails.getShopId() : "N/A"), startDate, endDate);

		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			// 1️⃣ Lấy dữ liệu
			List<ShopRevenueDTO> revenues = vendorService.getShopRevenue(shopId, startDate, endDate);
			log.info("Fetched {} revenue records for export.", revenues.size());

			// 2️⃣ Tạo Workbook & Sheet
			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("Doanh thu");

			// 3️⃣ Header Row
			Row headerRow = sheet.createRow(0);
			String[] headers = { "Ngày", "Số đơn hàng", "Doanh thu (Gross)", "Phí (Commission)", "Thực nhận (Net)" };

			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);
			headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerStyle.setAlignment(HorizontalAlignment.CENTER);

			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			// 4️⃣ Dòng dữ liệu
			int rowNum = 1;
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

			CreationHelper createHelper = workbook.getCreationHelper();

			CellStyle currencyCellStyle = workbook.createCellStyle();
			currencyCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0\" ₫\""));

			for (ShopRevenueDTO revenue : revenues) {
				Row row = sheet.createRow(rowNum++);

				row.createCell(0).setCellValue(revenue.getDate().format(dateFormatter));
				row.createCell(1).setCellValue(revenue.getTotalOrders());

				Cell grossCell = row.createCell(2);
				grossCell.setCellValue(revenue.getOrderAmount().doubleValue());
				grossCell.setCellStyle(currencyCellStyle);

				Cell commissionCell = row.createCell(3);
				commissionCell.setCellValue(revenue.getCommissionAmount().doubleValue());
				commissionCell.setCellStyle(currencyCellStyle);

				Cell netCell = row.createCell(4);
				netCell.setCellValue(revenue.getNetRevenue().doubleValue());
				netCell.setCellStyle(currencyCellStyle);
			}

			// 5️⃣ Tổng cộng
			Row totalRow = sheet.createRow(rowNum);
			Font totalFont = workbook.createFont();
			totalFont.setBold(true);

			CellStyle totalStyle = workbook.createCellStyle();
			totalStyle.setFont(totalFont);
			totalStyle.setDataFormat(currencyCellStyle.getDataFormat());

			Cell totalLabelCell = totalRow.createCell(1);
			totalLabelCell.setCellValue("Tổng cộng:");
			totalLabelCell.setCellStyle(headerStyle);

			double totalGross = revenues.stream().mapToDouble(r -> r.getOrderAmount().doubleValue()).sum();
			double totalCommission = revenues.stream().mapToDouble(r -> r.getCommissionAmount().doubleValue()).sum();
			double totalNet = revenues.stream().mapToDouble(r -> r.getNetRevenue().doubleValue()).sum();

			Cell totalGrossCell = totalRow.createCell(2);
			totalGrossCell.setCellValue(totalGross);
			totalGrossCell.setCellStyle(totalStyle);

			Cell totalCommissionCell = totalRow.createCell(3);
			totalCommissionCell.setCellValue(totalCommission);
			totalCommissionCell.setCellStyle(totalStyle);

			Cell totalNetCell = totalRow.createCell(4);
			totalNetCell.setCellValue(totalNet);
			totalNetCell.setCellStyle(totalStyle);

			// 6️⃣ Auto-size cột
			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			// 7️⃣ Gửi file về client
			String filename = "BaoCaoDoanhThu_"
					+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";
			response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

			workbook.write(response.getOutputStream());
			workbook.close();

			log.info("Successfully exported revenue data to Excel file: {}", filename);

		} catch (IllegalStateException e) {
			log.error("Authentication/Authorization error during export: {}", e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		} catch (IOException e) {
			log.error("IOException during Excel export: {}", e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			log.error("Unexpected error during Excel export", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
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
			List<ApprovalResponseDTO> approvals = vendorService.getPendingApprovals(shopId, entityType, actionType);

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