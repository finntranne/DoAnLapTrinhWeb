package com.alotra.controller; // Giữ package này

// Import các entity đã merge
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.user.User; // Sử dụng User

// Import Service và Repository đã merge
import com.alotra.model.ProductSaleDTO; 
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;
import com.alotra.service.shop.StoreService; // Import StoreService
import com.alotra.service.user.UserService; // Sử dụng UserService

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private CartService cartService;
    @Autowired private UserService userService;
    @Autowired private StoreService storeService; // Inject StoreService

    // Inner class Banner giữ nguyên
    public static class Banner {
        private String imageUrl;
        private String altText;
        public Banner(String u, String a) { this.imageUrl = u; this.altText = a; }
        public String getImageUrl() { return imageUrl; }
        public String getAltText() { return altText; }
    }

    // --- Hàm trợ giúp lấy số lượng giỏ hàng (Giữ nguyên sau sửa) ---
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
    
 // 1. ENDPOINT LƯU CHỌN LỰA CỦA NGƯỜI DÙNG VÀO SESSION
    @PostMapping("/select-shop/{shopId}")
    public ResponseEntity<?> selectShop(@PathVariable Integer shopId, HttpSession session) {
        // Lưu Shop ID vào Session (0 nghĩa là xem tất cả)
        session.setAttribute("selectedShopId", shopId);
        
        // Lấy tên và lưu vào Session
        String shopName = storeService.getShopNameById(shopId);
        session.setAttribute("selectedShopName", shopName); 
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/")
    public String home(Model model, @RequestParam(defaultValue = "0") int page, HttpSession session) {
        
        Integer selectedShopId = getSelectedShopId(session); // Lấy shopId

        System.out.println("DEBUG: selectedShopId from session = " + selectedShopId);
        
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", true);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        
        // Thêm dữ liệu Shop cho fragment
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));


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
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        // *** FIX LỖI: SỬ DỤNG findProductSaleData MỚI CÓ shopId ***
        if (isAuthenticated) {
            model.addAttribute("newestProducts", productService.findProductSaleData(selectedShopId, PageRequest.of(0, 10, newestSort)).getContent());
            model.addAttribute("topProducts", productService.findProductSaleData(selectedShopId, PageRequest.of(0, 10, topSellingSort)).getContent());
            model.addAttribute("topRatedProducts", productService.findProductSaleData(selectedShopId, PageRequest.of(0, 10, topRatedSort)).getContent());
            model.addAttribute("topLikedProducts", productService.findProductSaleData(selectedShopId, PageRequest.of(0, 10, topLikedSort)).getContent()); 
        }
        else {
            model.addAttribute("topProducts", productService.findProductSaleData(selectedShopId, PageRequest.of(0, 20, topSellingSort)).getContent());
        }
        	
        return "home/index";
    }

    // === SẢN PHẨM MỚI ===
    @GetMapping("/products/new")
    public String showNewProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            Model model, HttpSession session) { // Thêm HttpSession

        Integer selectedShopId = getSelectedShopId(session);

        int size = 2; 
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        // *** FIX LỖI: SỬ DỤNG findProductSaleData MỚI CÓ shopId ***
        model.addAttribute("newestProductPage", productService.findProductSaleData(selectedShopId, pageable));
        
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/new"); 
        
        // Thêm dữ liệu Shop
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));


        return "product/new-products";
    }
    
    // === SẢN PHẨM BÁN CHẠY ===
    @GetMapping("/products/best-selling")
    public String showBestSellingProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "bestSelling") String sort,
            Model model, HttpSession session) { // Thêm HttpSession

        Integer selectedShopId = getSelectedShopId(session);

        int size = 2;
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        // *** FIX LỖI: SỬ DỤNG findProductSaleData MỚI CÓ shopId ***
        model.addAttribute("bestSellingProductPage", productService.findProductSaleData(selectedShopId, pageable));
        
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/best-selling"); 
        
        // Thêm dữ liệu Shop
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

        return "product/best-selling-products";
    }
    
    // === SẢN PHẨM ĐÁNH GIÁ CAO ===
    @GetMapping("/products/top-rated")
    public String showTopRatedProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "topRated") String sort,
            Model model, HttpSession session) { // Thêm HttpSession

        Integer selectedShopId = getSelectedShopId(session);

        int size = 2;
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        // *** FIX LỖI: SỬ DỤNG findProductSaleData MỚI CÓ shopId ***
        model.addAttribute("topRatedProductPage", productService.findProductSaleData(selectedShopId, pageable));
        
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/top-rated"); 
        
        // Thêm dữ liệu Shop
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));


        return "product/top-rated-products";
    }
    
    // === SẢN PHẨM YÊU THÍCH ===
    @GetMapping("/products/top-liked")
    public String showTopLikedProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "topLiked") String sort,
            Model model, HttpSession session) { // Thêm HttpSession

        // Logic kiểm tra field totalLikes bị bỏ qua, giả định field này tồn tại nếu sử dụng sort=topLiked
        Integer selectedShopId = getSelectedShopId(session);

        int size = 2;
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        // *** FIX LỖI: SỬ DỤNG findProductSaleData MỚI CÓ shopId ***
        model.addAttribute("topLikedProductPage", productService.findProductSaleData(selectedShopId, pageable));
        
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/top-liked"); 
        
        // Thêm dữ liệu Shop
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));


        return "product/top-liked-products"; 
    }

    // === TRANG DANH MỤC ===
    @GetMapping("/categories/{categoryId}")
    public String showCategoryProductsPage(
            @PathVariable("categoryId") Integer categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            Model model, HttpSession session) { // Thêm HttpSession

        try {
            Integer selectedShopId = getSelectedShopId(session);

            Category category = categoryService.findById(categoryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));

            Sort sortOrder = getSort(sort);
            int size = 1; 
            Pageable pageable = PageRequest.of(page, size, sortOrder);
            
            // *** FIX LỖI: SỬ DỤNG findProductSaleDataByCategory MỚI CÓ shopId ***
            Page<ProductSaleDTO> productPage = productService.findProductSaleDataByCategory(category, selectedShopId, pageable);

            model.addAttribute("productPage", productPage);
            model.addAttribute("currentCategory", category);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("isHomePage", false);
            model.addAttribute("currentSort", sort);
            model.addAttribute("categoryId", categoryId);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("pagePath", "/categories/" + categoryId);
            
            // Thêm dữ liệu Shop
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));


            return "product/category-products";

        } catch (ResponseStatusException e) {
             return "redirect:/?error=category_not_found";
        }
    }

    // === Helper method để tạo Sort object (Giữ nguyên) ===
    private Sort getSort(String sortType) {
        if (sortType == null) sortType = "newest"; 
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