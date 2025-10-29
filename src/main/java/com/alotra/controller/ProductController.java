package com.alotra.controller; // Giữ package này

// Import entity đã merge
import com.alotra.entity.product.Product;
import com.alotra.entity.product.Topping;
import com.alotra.entity.product.Review;
import com.alotra.entity.user.User;

// Import Service và Repository đã merge
import com.alotra.model.ProductSaleDTO; 
import com.alotra.repository.product.ToppingRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;
import com.alotra.service.shop.StoreService;
import com.alotra.service.user.ReviewService;
import com.alotra.service.user.UserService;

import jakarta.servlet.http.HttpSession; // *** THÊM IMPORT NÀY ***

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductController {

	@Autowired
	private ProductService productService;
	@Autowired
	private CategoryService categoryService;
	@Autowired
	private ReviewService reviewService;
	@Autowired
	private ToppingRepository toppingRepository;
	@Autowired
	private CartService cartService;
	@Autowired
	private UserService userService;
	@Autowired private StoreService storeService; // Inject StoreService
    // Cần thêm StoreService nếu muốn truyền danh sách Shops và tên shop đã chọn vào view

    // --- Hàm trợ giúp lấy số lượng giỏ hàng (Giữ nguyên) ---
	private int getCurrentCartItemCount() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
			String username = auth.getName(); 
			try {
				User user = userService.findByUsername(username)
						.orElseThrow(() -> new UsernameNotFoundException("User not found during cart count"));
				return cartService.getCartItemCount(user);
			} catch (UsernameNotFoundException e) {
				System.err.println("Error getting cart count: " + e.getMessage());
				return 0;
			}
		}
		return 0;
	}
    
    // --- Helper Lấy Shop ID từ Session ---
    private Integer getSelectedShopId(HttpSession session) {
        Integer selectedShopId = (Integer) session.getAttribute("selectedShopId");
        return (selectedShopId == null) ? 0 : selectedShopId; // Mặc định là 0 (Xem tất cả)
    }

	@GetMapping("/products/{id}")
	// *** SỬA: THÊM HttpSession session ***
	public String getProductDetail(@PathVariable("id") Integer id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {

		try {
            // 1. Đồng bộ Shop ID (Khắc phục lỗi mất shop)
			Integer selectedShopId = getSelectedShopId(session);
			if (selectedShopId == 0) {
		        // 1. Thêm thông báo lỗi vào Flash Attribute
		        redirectAttributes.addFlashAttribute(
		            "errorMessage", // ✅ Tên attribute mà view sẽ sử dụng
		            "❌ Vui lòng chọn một cửa hàng để xem chi tiết sản phẩm."
		        );
		        
		        // 2. Chuyển hướng về trang chủ
		        return "redirect:/"; 
		    }
			// 2. Lấy DTO chính của sản phẩm (Lọc theo shop ID đã chọn)
			ProductSaleDTO saleDTO = productService.findProductSaleDataById(id, selectedShopId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));

			Product product = saleDTO.getProduct();
			if (product == null) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dữ liệu sản phẩm không hợp lệ.");
			}

			// 3. Lấy danh sách đánh giá
			Pageable reviewPageable = PageRequest.of(0, 5); 
			Page<Review> reviewPage = reviewService.findByProduct(product, reviewPageable); 

			// 4. Lấy sản phẩm liên quan (cùng danh mục, lọc theo shop ID đã chọn)
			Pageable relatedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "soldCount"));
			Page<ProductSaleDTO> relatedProducts = productService
					.findProductSaleDataByCategory(product.getCategory(), selectedShopId, relatedPageable);

            // 5. LỌC TOPPING THEO SẢN PHẨM VÀ SHOP
            
            // Lấy tất cả Topping ACTIVE có sẵn tại shop mà sản phẩm thuộc về (hoặc topping chung)
            Integer productShopId = product.getShop().getShopId();
            List<Topping> availableShopToppings;
            
            if (productShopId != null && productShopId != 0) {
                // Sản phẩm thuộc shop cụ thể -> Lấy topping của shop đó
                availableShopToppings = toppingRepository.findAllActiveToppingsByShop(productShopId);
            } else {
                // Sản phẩm Admin (ShopID NULL/0) -> Lấy topping chung
                availableShopToppings = toppingRepository.findByStatusAndShopIsNull((byte) 1);
            }
            
            // Lấy ID của các Topping được gán cho sản phẩm (Many-to-Many)
            Set<Integer> productAssignedToppingIds = product.getAvailableToppings().stream()
                .map(Topping::getToppingID)
                .collect(Collectors.toSet());
                
            // Lọc ra những topping thỏa mãn cả 2 điều kiện: ACTIVE tại Shop VÀ được gán cho Sản phẩm
            List<Topping> filteredToppings = availableShopToppings.stream()
                .filter(topping -> productAssignedToppingIds.contains(topping.getToppingID()))
                .collect(Collectors.toList());


			// 6. Đưa tất cả vào model
			model.addAttribute("saleDTO", saleDTO);
			model.addAttribute("product", product);
			model.addAttribute("variants", product.getVariants()); 
			model.addAttribute("reviews", reviewPage.getContent());
			model.addAttribute("relatedProducts", relatedProducts.getContent());
			model.addAttribute("cartItemCount", getCurrentCartItemCount());

			// ✅ CHỈ LẤY TOPPING ĐƯỢC PHÉP CHO SẢN PHẨM
			model.addAttribute("toppings", filteredToppings != null ? filteredToppings : Collections.emptyList());

			model.addAttribute("categories", categoryService.findAll());
			model.addAttribute("isHomePage", false);
			
			// Thêm dữ liệu Shop cho fragment
	        model.addAttribute("shops", storeService.findAllActiveShops());
	        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
            model.addAttribute("shopId", selectedShopId); // ✅ THỐNG NHẤT TÊN CHO VIEW

			return "product/detail";


		} catch (ResponseStatusException e) {
			System.err.println("Error loading product detail: " + e.getMessage());
			return "redirect:/?error=product_not_found";
		} catch (Exception e) {
			System.err.println("Unexpected error loading product detail: " + e.getMessage());
			e.printStackTrace();
			return "redirect:/?error=internal_error";
		}
	}

	@GetMapping("/search")
	// *** SỬA: THÊM HttpSession session ***
	public String searchProducts(@RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "sort", required = false, defaultValue = "priceDesc") String sort, Model model, HttpSession session) {

        Integer selectedShopId = getSelectedShopId(session);

        try {
			model.addAttribute("cartItemCount", getCurrentCartItemCount());
			model.addAttribute("categories", categoryService.findAll());
			
			// Thêm dữ liệu Shop cho fragment
	        model.addAttribute("shops", storeService.findAllActiveShops());
	        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
            model.addAttribute("shopId", selectedShopId); // ✅ THỐNG NHẤT TÊN
		} catch (Exception e) {
			System.err.println("Error preparing search page model: " + e.getMessage());
		}

		int pageSize = 2; 
		Sort sortOrder;
		try {
			sortOrder = SortHelper.getSort(sort); 
		} catch (IllegalArgumentException ex) {
			System.err.println("Invalid sort parameter: " + sort + ". Defaulting to 'newest'.");
			sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
			sort = "newest"; 
		}

		Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

		Page<ProductSaleDTO> salePage = productService.findProductSaleDataByKeyword(keyword, selectedShopId, pageable);

		model.addAttribute("sales", salePage);
		model.addAttribute("keyword", keyword);
		model.addAttribute("currentSort", sort);
		model.addAttribute("totalItems", salePage.getTotalElements());
		model.addAttribute("isHomePage", false);
		model.addAttribute("pagePath", "/search");

		return "shop/search_results";
	}

	// === Helper method để tạo Sort object (Giữ nguyên) ===
	private static class SortHelper {
		private static Sort getSort(String sortType) {
			if (sortType == null)
				sortType = "newest"; 

			switch (sortType) {
			case "priceAsc":
				return Sort.by(Sort.Direction.ASC, "basePrice");
			case "priceDesc":
				return Sort.by(Sort.Direction.DESC, "basePrice");
			case "nameAsc":
				return Sort.by(Sort.Direction.ASC, "productName");
			case "nameDesc":
				return Sort.by(Sort.Direction.DESC, "productName");
			case "bestSelling":
				return Sort.by(Sort.Direction.DESC, "soldCount");
			case "topRated":
				return Sort.by(Sort.Direction.DESC, "averageRating").and(Sort.by(Sort.Direction.DESC, "totalReviews"));
			case "newest":
			default:
				return Sort.by(Sort.Direction.DESC, "createdAt");
			}
		}
	}

}