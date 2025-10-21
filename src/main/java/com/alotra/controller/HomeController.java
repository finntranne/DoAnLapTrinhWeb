package com.alotra.controller;

import com.alotra.entity.product.Category;
import com.alotra.entity.user.Customer;
import com.alotra.entity.user.User;
import com.alotra.model.ProductSaleDTO;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;
import com.alotra.service.user.CustomerService;
import com.alotra.service.user.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;
    
    @Autowired private CartService cartService;
    
    @Autowired @Qualifier("userServiceImpl") private UserService userService; // Giữ nguyên
    
    @Autowired private CustomerService customerService; // Giữ nguyên

    // ... (Class Banner giữ nguyên) ...
    public static class Banner {
        private String imageUrl;
        private String altText;
        public Banner(String u, String a) { this.imageUrl = u; this.altText = a; }
        public String getImageUrl() { return imageUrl; }
        public String getAltText() { return altText; }
    }
    
 // --- Hàm trợ giúp lấy số lượng giỏ hàng ---
    private int getCurrentCartItemCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            Optional<User> userOpt = userService.findByUsername(username); // Hoặc findByEmail
            if (userOpt.isPresent()) {
                Optional<Customer> customerOpt = customerService.findByUser(userOpt.get());
                if (customerOpt.isPresent()) {
                    return cartService.getCartItemCount(customerOpt.get());
                }
            }
        }
        return 0; // Trả về 0 nếu chưa đăng nhập hoặc có lỗi
    }

    @GetMapping("/")
    public String home(Model model, @RequestParam(defaultValue = "0") int page) {
        // ... (Code trang chủ giữ nguyên) ...
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", true);
        
        model.addAttribute("cartItemCount", getCurrentCartItemCount());

        List<Banner> banners = new ArrayList<>();
        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/09/cover-web-khe%CC%82%CC%81-scaled.jpg", "Banner PhinDi"));
        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/08/cover-web-warabi-scaled.jpg", "Banner Trà"));
        model.addAttribute("banners", banners);
        
        Sort topSellingSort = Sort.by(Sort.Direction.DESC, "soldCount");
        Sort newestSort = Sort.by(Sort.Direction.DESC, "createdAt");
        Sort topRatedSort = Sort.by(Sort.Direction.DESC, "averageRating")
                                .and(Sort.by(Sort.Direction.DESC, "totalReviews"));
        Sort topLikedSort = Sort.by(Sort.Direction.DESC, "totalLikes");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            model.addAttribute("newestProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, newestSort)).getContent());
            model.addAttribute("topProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topSellingSort)).getContent());
            model.addAttribute("topRatedProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topRatedSort)).getContent());
            model.addAttribute("topLikedProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topLikedSort)).getContent());
        }
        else {
            model.addAttribute("topProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 20, topSellingSort)).getContent());
        }
        	
        return "home/index";
    }

    // === CẬP NHẬT 5 PHƯƠNG THỨC DƯỚI ĐÂY ===

    @GetMapping("/products/new")
    public String showNewProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "newest") String sortType, // Thêm
            Model model) {
        
        int size = 20; // Sửa size
        Sort sort = getSort(sortType); // Gọi hàm helper
        PageRequest pageable = PageRequest.of(page, size, sort);

        model.addAttribute("newestProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        
        model.addAttribute("currentSort", sortType); // Thêm
        model.addAttribute("baseUrl", "/products/new"); // Thêm
        
        return "product/new-products";
    }
    
    @GetMapping("/products/best-selling")
    public String showBestSellingProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "bestSelling") String sortType, // Thêm
            Model model) {
        
        int size = 20; // Sửa size
        Sort sort = getSort(sortType); // Gọi hàm helper
        PageRequest pageable = PageRequest.of(page, size, sort);

        model.addAttribute("bestSellingProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        
        model.addAttribute("currentSort", sortType); // Thêm
        model.addAttribute("baseUrl", "/products/best-selling"); // Thêm

        return "product/best-selling-products";
    }
    
    @GetMapping("/products/top-rated")
    public String showTopRatedProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "topRated") String sortType, // Thêm
            Model model) {
        
        int size = 20; // Sửa size
        Sort sort = getSort(sortType); // Gọi hàm helper
        PageRequest pageable = PageRequest.of(page, size, sort);

        model.addAttribute("topRatedProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        
        model.addAttribute("currentSort", sortType); // Thêm
        model.addAttribute("baseUrl", "/products/top-rated"); // Thêm

        return "product/top-rated-products";
    }
    
    @GetMapping("/products/top-liked")
    public String showTopLikedProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "topLiked") String sortType, // Thêm
            Model model) {
        
        int size = 20; // Sửa size
        Sort sort = getSort(sortType); // Gọi hàm helper
        PageRequest pageable = PageRequest.of(page, size, sort);

        model.addAttribute("topLikedProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        
        model.addAttribute("currentSort", sortType); // Thêm
        model.addAttribute("baseUrl", "/products/top-liked"); // Thêm

        return "product/top-liked-products";
    }
    
    @GetMapping("/categories/{categoryId}")
    public String showCategoryProductsPage(
            @PathVariable("categoryId") Integer categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "newest") String sortType, // Giữ nguyên
            Model model) {

        Category category = categoryService.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        Sort sort = getSort(sortType); // Gọi hàm helper
        int size = 20; // Sửa size
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductSaleDTO> productPage = productService.findProductSaleDataByCategoryPaginated(category, pageable);

        model.addAttribute("productPage", productPage);
        model.addAttribute("currentCategory", category);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("currentSort", sortType); 
        model.addAttribute("baseUrl", "/categories/" + categoryId);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());

        return "product/category-products";
    }
    
    
    /**
     * === HÀM HELPER MỚI ===
     * Tạo đối tượng Sort dựa trên sortType
     */
    private Sort getSort(String sortType) {
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
                return Sort.by(Sort.Direction.DESC, "averageRating")
                           .and(Sort.by(Sort.Direction.DESC, "totalReviews"));
            case "topLiked":
                return Sort.by(Sort.Direction.DESC, "totalLikes");
            case "newest":
            default:
                return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }
}