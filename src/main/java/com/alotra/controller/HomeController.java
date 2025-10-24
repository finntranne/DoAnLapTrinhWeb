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
    
    @Autowired @Qualifier("userServiceImpl") private UserService userService;
    
    @Autowired private CustomerService customerService;

    public static class Banner {
        private String imageUrl;
        private String altText;
        public Banner(String u, String a) { this.imageUrl = u; this.altText = a; }
        public String getImageUrl() { return imageUrl; }
        public String getAltText() { return altText; }
    }
    
    private int getCurrentCartItemCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                Optional<Customer> customerOpt = customerService.findByUser(userOpt.get());
                if (customerOpt.isPresent()) {
                    return cartService.getCartItemCount(customerOpt.get());
                }
            }
        }
        return 0;
    }

    @GetMapping("/")
    public String home(Model model, @RequestParam(defaultValue = "0") int page) {
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

    // === SẢN PHẨM MỚI ===
    @GetMapping("/products/new")
    public String showNewProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            Model model) {
        
        int size = 2; // 3 rows x 5 columns
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        model.addAttribute("newestProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        
        return "product/new-products";
    }
    
    // === SẢN PHẨM BÁN CHẠY ===
    @GetMapping("/products/best-selling")
    public String showBestSellingProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "bestSelling") String sort,
            Model model) {
        
        int size = 2;
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        model.addAttribute("bestSellingProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);

        return "product/best-selling-products";
    }
    
    // === SẢN PHẨM ĐÁNH GIÁ CAO ===
    @GetMapping("/products/top-rated")
    public String showTopRatedProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "topRated") String sort,
            Model model) {
        
        int size = 2;
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        model.addAttribute("topRatedProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);

        return "product/top-rated-products";
    }
    
    // === SẢN PHẨM YÊU THÍCH ===
    @GetMapping("/products/top-liked")
    public String showTopLikedProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "topLiked") String sort,
            Model model) {
        
        int size = 2;
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        model.addAttribute("topLikedProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);

        return "product/top-liked-products";
    }
    
    // === TRANG DANH MỤC ===
    @GetMapping("/categories/{categoryId}")
    public String showCategoryProductsPage(
            @PathVariable("categoryId") Integer categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            Model model) {

        Category category = categoryService.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        Sort sortOrder = getSort(sort);
        int size = 1;
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<ProductSaleDTO> productPage = productService.findProductSaleDataByCategoryPaginated(category, pageable);

        model.addAttribute("productPage", productPage);
        model.addAttribute("currentCategory", category);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("currentSort", sort);
        model.addAttribute("categoryId", categoryId); // Thêm để pagination biết
        model.addAttribute("cartItemCount", getCurrentCartItemCount());

        return "product/category-products";
    }
    
    /**
     * Helper method để tạo Sort object
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