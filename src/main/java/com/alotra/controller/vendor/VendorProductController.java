package com.alotra.controller.vendor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.dto.product.ProductStatisticsDTO;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.Topping;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorProductController {
	private final VendorProductService vendorProductService;
	private final ToppingRepository toppingRepository;
	private final PromotionRepository promotionRepository;

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

	// ==================== PRODUCT MANAGEMENT ====================
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
			Page<ProductStatisticsDTO> products = vendorProductService.getShopProducts(shopId, status, categoryId,
					approvalStatus, search, pageable);

			model.addAttribute("products", products);
			model.addAttribute("currentPage", page);
			model.addAttribute("totalPages", products.getTotalPages());
			model.addAttribute("status", status);
			model.addAttribute("search", search);
			model.addAttribute("categoryId", categoryId);
			// *** ADD approvalStatus TO MODEL HERE ***
			model.addAttribute("approvalStatus", approvalStatus);
			model.addAttribute("categories", vendorProductService.getAllCategoriesSimple());

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
			model.addAttribute("categories", vendorProductService.getAllCategories());
			model.addAttribute("sizes", vendorProductService.getAllSizesSimple());
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
			model.addAttribute("categories", vendorProductService.getAllCategories());
			model.addAttribute("sizes", vendorProductService.getAllSizesSimple());
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
			vendorProductService.requestProductCreation(shopId, request, userId, selectedToppings);

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

			Product product = vendorProductService.getProductDetail(shopId, id);
			ProductRequestDTO dto = vendorProductService.convertProductToDTO(product);

			model.addAttribute("product", dto);
			model.addAttribute("action", "edit");
			model.addAttribute("productId", id);

			model.addAttribute("categories", vendorProductService.getAllCategories());
			model.addAttribute("sizes", vendorProductService.getAllSizesSimple());
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
				Product product = vendorProductService.getProductDetail(shopId, id);
				model.addAttribute("categories", vendorProductService.getAllCategories());
				model.addAttribute("sizes", vendorProductService.getAllSizesSimple());
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
			vendorProductService.requestProductUpdate(shopId, request, userId, selectedToppings);
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

			vendorProductService.requestProductDeletion(shopId, id, userId);

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

}
