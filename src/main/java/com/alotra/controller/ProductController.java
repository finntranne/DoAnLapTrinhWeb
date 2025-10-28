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
	public String getProductDetail(@PathVariable("id") Integer id, Model model, HttpSession session) {
		try {
            // Lấy shopId từ Session
            Integer selectedShopId = getSelectedShopId(session);

			// 1. Lấy DTO chính của sản phẩm
            // *** FIX LỖI: THÊM selectedShopId VÀO findProductSaleDataById ***
			ProductSaleDTO saleDTO = productService.findProductSaleDataById(id, selectedShopId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));

			Product product = saleDTO.getProduct();
			if (product == null) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dữ liệu sản phẩm không hợp lệ.");
			}

			// 2. Lấy danh sách đánh giá
			Pageable reviewPageable = PageRequest.of(0, 5); 
			Page<Review> reviewPage = reviewService.findByProduct(product, reviewPageable); 

			// 3. Lấy sản phẩm liên quan (cùng danh mục)
			Pageable relatedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "soldCount"));
            // *** FIX LỖI: THÊM selectedShopId VÀO findProductSaleDataByCategoryPaginated ***
			Page<ProductSaleDTO> relatedProducts = productService
					.findProductSaleDataByCategory(product.getCategory(), selectedShopId, relatedPageable); // Sửa tên hàm

			// 4. Đưa tất cả vào model
			model.addAttribute("saleDTO", saleDTO);
			model.addAttribute("product", product);
			model.addAttribute("variants", product.getVariants()); 
			model.addAttribute("reviews", reviewPage.getContent());
			model.addAttribute("relatedProducts", relatedProducts.getContent());
			model.addAttribute("cartItemCount", getCurrentCartItemCount());

			List<Topping> activeToppings = toppingRepository.findAllActiveToppings();
			model.addAttribute("toppings", activeToppings != null ? activeToppings : Collections.emptyList());

			model.addAttribute("categories", categoryService.findAll());
			model.addAttribute("isHomePage", false);
            
            // NOTE: Cần truyền thông tin Shop vào Model nếu muốn hiển thị tên Shop đã chọn trên thanh Navbar

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

        // *** FIX LỖI: THÊM selectedShopId VÀO findProductSaleDataByKeyword ***
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