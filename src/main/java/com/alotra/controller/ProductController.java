//package com.alotra.controller;
//
//import com.alotra.entity.product.Product;
//import com.alotra.entity.product.Topping;
//import com.alotra.entity.user.Customer;
//import com.alotra.entity.user.Review;
//import com.alotra.entity.user.User;
//import com.alotra.model.ProductSaleDTO;
//import com.alotra.service.cart.CartService;
//import com.alotra.service.product.CategoryService;
//import com.alotra.service.product.ProductService;
//import com.alotra.service.user.CustomerService;
//import com.alotra.service.user.ReviewService;
//import com.alotra.service.user.UserService;
//
//import java.util.Optional;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.server.ResponseStatusException;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//@Controller
//public class ProductController {
//
//    @Autowired
//    private ProductService productService;
//    
//    @Autowired
//    private CategoryService categoryService; // Cần cho breadcrumb
//
//    @Autowired
//    private ReviewService reviewService; // Cần cho đánh giá
//    
//    @Autowired private CartService cartService;
//    
//    @Autowired @Qualifier("userServiceImpl") private UserService userService; // Giữ nguyên
//    
//    @Autowired private CustomerService customerService; // Giữ nguyên
//    
// // --- Hàm trợ giúp lấy số lượng giỏ hàng ---
//    private int getCurrentCartItemCount() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
//            String username = auth.getName();
//            Optional<User> userOpt = userService.findByUsername(username); // Hoặc findByEmail
//            if (userOpt.isPresent()) {
//                Optional<Customer> customerOpt = customerService.findByUser(userOpt.get());
//                if (customerOpt.isPresent()) {
//                    return cartService.getCartItemCount(customerOpt.get());
//                }
//            }
//        }
//        return 0; // Trả về 0 nếu chưa đăng nhập hoặc có lỗi
//    }
//
//    @GetMapping("/products/{id}")
//    public String getProductDetail(@PathVariable("id") Integer id, Model model) {
//        
//        // 1. Lấy DTO chính của sản phẩm
//        ProductSaleDTO saleDTO = productService.findProductSaleDataById(id)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
//        
//        Product product = saleDTO.getProduct();
//        
//        // 2. Lấy danh sách đánh giá (ví dụ: 5 đánh giá đầu tiên)
//        Page<Review> reviewPage = reviewService.findByProduct(product, PageRequest.of(0, 5));
//        
//        // 3. Lấy sản phẩm liên quan (cùng danh mục, 5 sản phẩm)
//        Page<ProductSaleDTO> relatedProducts = productService.findProductSaleDataByCategoryPaginated(
//            product.getCategory(), 
//            PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "soldCount"))
//        );
//
//        // 4. Đưa tất cả vào model
//        model.addAttribute("saleDTO", saleDTO);
//        model.addAttribute("product", product);
//        model.addAttribute("variants", product.getProductVariants()); // Cho JS chọn size
//        model.addAttribute("reviews", reviewPage.getContent());
//        model.addAttribute("relatedProducts", relatedProducts.getContent());
//        
//        model.addAttribute("cartItemCount", getCurrentCartItemCount());
//        
//     // --- PASS TOPPINGS TO VIEW ---
//        // Filter only active toppings if needed
//        Set<Topping> activeToppings = product.getAvailableToppings().stream()
//                                          .filter(t -> t.getStatus() != null && t.getStatus() == 1)
//                                          .collect(Collectors.toSet());
//        model.addAttribute("toppings", activeToppings);
//        // -----------------------------
//        
//        // Cần cho layout
//        model.addAttribute("categories", categoryService.findAll()); 
//        model.addAttribute("isHomePage", false);
//
//        return "product/detail"; // Trả về file view mới
//    }
//    
//    @GetMapping("/search")
//    public String searchProducts(
//            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
//            @RequestParam(name = "page", defaultValue = "0") int page, // ⚠️ ĐỔI THÀNH 0-indexed
//            @RequestParam(name = "sort", required = false, defaultValue = "priceDesc") String sort,
//            Model model) {
//
//        try {
//            model.addAttribute("cartItemCount", getCurrentCartItemCount());
//            model.addAttribute("categories", categoryService.findAll());
//        } catch (Exception e) { /* Handle or ignore */ }
//
//        int pageSize = 2; // Recommended: 3 rows x 5 columns
//
//        // === CREATE SORT OBJECT ===
//        Sort sortOrder;
//        switch (sort) {
//            case "nameAsc":
//                sortOrder = Sort.by("productName").ascending(); 
//                break;
//            case "nameDesc":
//                sortOrder = Sort.by("productName").descending();
//                break;
//            case "priceAsc":
//                sortOrder = Sort.by("basePrice").ascending(); 
//                break;
//            case "priceDesc":
//            default:
//                sortOrder = Sort.by("basePrice").descending(); 
//                sort = "priceDesc"; 
//                break;
//        }
//
//        Pageable pageable = PageRequest.of(page, pageSize, sortOrder); // page đã là 0-indexed
//
//        Page<ProductSaleDTO> salePage = productService.findProductSaleDataByKeyword(keyword, pageable);
//
//        // Pass data to View
//        model.addAttribute("sales", salePage);
//        model.addAttribute("keyword", keyword);
//        model.addAttribute("currentSort", sort);
//        model.addAttribute("totalItems", salePage.getTotalElements());
//        model.addAttribute("isHomePage", false);
//
//        return "shop/search_results";
//    }
//}

package com.alotra.controller; // Giữ package này

// Import entity đã merge
import com.alotra.entity.product.Product;
import com.alotra.entity.product.Topping;
// import com.alotra.entity.user.Customer; // *** BỎ Customer ***
import com.alotra.entity.product.Review;
import com.alotra.entity.user.User; // Sử dụng User

// Import Service và Repository đã merge
import com.alotra.model.ProductSaleDTO; // Giữ DTO này nếu ProductRepository còn dùng
import com.alotra.repository.product.ToppingRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;
// import com.alotra.service.user.CustomerService; // *** BỎ CustomerService ***
import com.alotra.service.user.ReviewService;
import com.alotra.service.user.UserService; // Sử dụng UserService

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Qualifier; // Bỏ nếu không cần
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Thêm exception
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
	private ReviewService reviewService; // Cần cho đánh giá
	@Autowired
	private ToppingRepository toppingRepository;
	@Autowired
	private CartService cartService;
	// @Autowired @Qualifier("userServiceImpl") // Bỏ nếu không cần
	@Autowired
	private UserService userService; // *** GIỮ UserService ***
	// @Autowired private CustomerService customerService; // *** BỎ CustomerService
	// ***

	// --- Hàm trợ giúp lấy số lượng giỏ hàng (ĐÃ SỬA) ---
	private int getCurrentCartItemCount() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
			String username = auth.getName(); // Thường là email
			try {
				// *** SỬA: Tìm User trực tiếp ***
				User user = userService.findByUsername(username)
						.orElseThrow(() -> new UsernameNotFoundException("User not found during cart count"));
				// Gọi cartService với User
				return cartService.getCartItemCount(user);
			} catch (UsernameNotFoundException e) {
				System.err.println("Error getting cart count: " + e.getMessage());
				return 0; // User không tìm thấy
			}
		}
		return 0; // Chưa đăng nhập
	}

	@GetMapping("/products/{id}")
	public String getProductDetail(@PathVariable("id") Integer id, Model model) {
		try {
			// 1. Lấy DTO chính của sản phẩm (giả sử method này tồn tại)
			ProductSaleDTO saleDTO = productService.findProductSaleDataById(id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));

			Product product = saleDTO.getProduct(); // Lấy Product entity từ DTO

			// Kiểm tra Product có null không (phòng trường hợp DTO lỗi)
			if (product == null) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dữ liệu sản phẩm không hợp lệ.");
			}

			// 2. Lấy danh sách đánh giá
			// *** SỬA: ReviewService cần phương thức findByProduct ***
			Pageable reviewPageable = PageRequest.of(0, 5); // Ví dụ: 5 đánh giá mới
																								// nhất
			Page<Review> reviewPage = reviewService.findByProduct(product, reviewPageable); // Cần có method này

			// 3. Lấy sản phẩm liên quan (cùng danh mục)
			Pageable relatedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "soldCount"));
			// Giả sử ProductService có phương thức này
			Page<ProductSaleDTO> relatedProducts = productService
					.findProductSaleDataByCategoryPaginated(product.getCategory(), relatedPageable);

			// 4. Đưa tất cả vào model
			model.addAttribute("saleDTO", saleDTO);
			model.addAttribute("product", product);
			model.addAttribute("variants", product.getVariants()); // Lấy variants từ Product entity đã merge
			model.addAttribute("reviews", reviewPage.getContent());
			model.addAttribute("relatedProducts", relatedProducts.getContent());
			model.addAttribute("cartItemCount", getCurrentCartItemCount()); // Đã sửa

			List<Topping> activeToppings = toppingRepository.findAllActiveToppings(); // Use repo method
			model.addAttribute("toppings", activeToppings != null ? activeToppings : Collections.emptyList());

			model.addAttribute("categories", categoryService.findAll());
			model.addAttribute("isHomePage", false);

			return "product/detail"; // View detail.html

		} catch (ResponseStatusException e) {
			// Log lỗi 404 hoặc lỗi khác
			System.err.println("Error loading product detail: " + e.getMessage());
			// Redirect về trang chủ hoặc trang lỗi tùy ý
			return "redirect:/?error=product_not_found";
		} catch (Exception e) {
			// Log lỗi không mong muốn
			System.err.println("Unexpected error loading product detail: " + e.getMessage());
			e.printStackTrace();
			return "redirect:/?error=internal_error";
		}
	}

	@GetMapping("/search")
	public String searchProducts(@RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "sort", required = false, defaultValue = "priceDesc") String sort, Model model) {

		try {
			model.addAttribute("cartItemCount", getCurrentCartItemCount()); // Đã sửa
			model.addAttribute("categories", categoryService.findAll());
		} catch (Exception e) {
			System.err.println("Error preparing search page model: " + e.getMessage());
			// Có thể bỏ qua lỗi này nếu không quá nghiêm trọng
		}

		int pageSize = 2; // Ví dụ: 3 rows x 5 columns
		Sort sortOrder;
		try {
			// Sử dụng hàm getSort từ HomeController (cần đảm bảo nó tồn tại hoặc copy sang
			// đây)
			// Hoặc định nghĩa lại logic sort ở đây
			sortOrder = SortHelper.getSort(sort); // Giả sử có lớp SortHelper
		} catch (IllegalArgumentException ex) {
			System.err.println("Invalid sort parameter: " + sort + ". Defaulting to 'newest'.");
			sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
			sort = "newest"; // Cập nhật lại sort để hiển thị đúng trên view
		}

		Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

		// Giả sử ProductService có phương thức này và trả về Page<ProductSaleDTO>
		Page<ProductSaleDTO> salePage = productService.findProductSaleDataByKeyword(keyword, pageable);

		model.addAttribute("sales", salePage);
		model.addAttribute("keyword", keyword);
		model.addAttribute("currentSort", sort);
		model.addAttribute("totalItems", salePage.getTotalElements());
		model.addAttribute("isHomePage", false);
		model.addAttribute("pagePath", "/search"); // Path cho pagination/sort

		return "shop/search_results"; // Dùng view chung
	}

	// === Cần copy hoặc tạo lớp SortHelper nếu dùng ===
	private static class SortHelper {
		private static Sort getSort(String sortType) {
			if (sortType == null)
				sortType = "newest"; // Default if null

			switch (sortType) {
			case "priceAsc":
				// *** WARNING: 'basePrice' might not exist on Product/ProductSaleDTO ***
				// If 'basePrice' was removed during merge, you need to change this.
				// Perhaps sort by variant price? That requires a different query structure.
				// For now, keeping it as 'basePrice' assuming it exists OR ProductSaleDTO still
				// has it.
				return Sort.by(Sort.Direction.ASC, "basePrice");
			case "priceDesc":
				// *** WARNING: 'basePrice' might not exist on Product/ProductSaleDTO ***
				return Sort.by(Sort.Direction.DESC, "basePrice");
			case "nameAsc":
				return Sort.by(Sort.Direction.ASC, "productName");
			case "nameDesc":
				return Sort.by(Sort.Direction.DESC, "productName");
			case "bestSelling":
				return Sort.by(Sort.Direction.DESC, "soldCount");
			case "topRated":
				return Sort.by(Sort.Direction.DESC, "averageRating").and(Sort.by(Sort.Direction.DESC, "totalReviews"));
			// case "topLiked": // Removed as 'totalLikes' likely doesn't exist
			// return Sort.by(Sort.Direction.DESC, "totalLikes");
			case "newest":
			default:
				return Sort.by(Sort.Direction.DESC, "createdAt");
			}
		}
	}

}