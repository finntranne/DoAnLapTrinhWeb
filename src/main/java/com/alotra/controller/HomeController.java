//package com.alotra.controller;
//
//import com.alotra.entity.product.Category;
//import com.alotra.entity.user.Customer;
//import com.alotra.entity.user.User;
//import com.alotra.model.ProductSaleDTO;
//import com.alotra.service.cart.CartService;
//import com.alotra.service.product.CategoryService;
//import com.alotra.service.product.ProductService;
//import com.alotra.service.user.CustomerService;
//import com.alotra.service.user.UserService;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.web.server.ResponseStatusException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;

//@Controller
//public class HomeController {
//
//    @Autowired
//    private ProductService productService;
//
//    @Autowired
//    private CategoryService categoryService;
//    
//    @Autowired private CartService cartService;
//    
//    @Autowired @Qualifier("userServiceImpl") private UserService userService;
//    
//    @Autowired private CustomerService customerService;
//
//    public static class Banner {
//        private String imageUrl;
//        private String altText;
//        public Banner(String u, String a) { this.imageUrl = u; this.altText = a; }
//        public String getImageUrl() { return imageUrl; }
//        public String getAltText() { return altText; }
//    }
//    
//    private int getCurrentCartItemCount() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
//            String username = auth.getName();
//            Optional<User> userOpt = userService.findByUsername(username);
//            if (userOpt.isPresent()) {
//                Optional<Customer> customerOpt = customerService.findByUser(userOpt.get());
//                if (customerOpt.isPresent()) {
//                    return cartService.getCartItemCount(customerOpt.get());
//                }
//            }
//        }
//        return 0;
//    }
//
//    @GetMapping("/")
//    public String home(Model model, @RequestParam(defaultValue = "0") int page) {
//        model.addAttribute("categories", categoryService.findAll());
//        model.addAttribute("isHomePage", true);
//        model.addAttribute("cartItemCount", getCurrentCartItemCount());
//
//        List<Banner> banners = new ArrayList<>();
//        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/09/cover-web-khe%CC%82%CC%81-scaled.jpg", "Banner PhinDi"));
//        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/08/cover-web-warabi-scaled.jpg", "Banner Trà"));
//        model.addAttribute("banners", banners);
//        
//        Sort topSellingSort = Sort.by(Sort.Direction.DESC, "soldCount");
//        Sort newestSort = Sort.by(Sort.Direction.DESC, "createdAt");
//        Sort topRatedSort = Sort.by(Sort.Direction.DESC, "averageRating")
//                                .and(Sort.by(Sort.Direction.DESC, "totalReviews"));
//        Sort topLikedSort = Sort.by(Sort.Direction.DESC, "totalLikes");
//
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
//            model.addAttribute("newestProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, newestSort)).getContent());
//            model.addAttribute("topProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topSellingSort)).getContent());
//            model.addAttribute("topRatedProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topRatedSort)).getContent());
//            model.addAttribute("topLikedProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topLikedSort)).getContent());
//        }
//        else {
//            model.addAttribute("topProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 20, topSellingSort)).getContent());
//        }
//        	
//        return "home/index";
//    }
//
//    // === SẢN PHẨM MỚI ===
//    @GetMapping("/products/new")
//    public String showNewProductsPage(
//            @RequestParam(name = "page", defaultValue = "0") int page,
//            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
//            Model model) {
//        
//        int size = 2; // 3 rows x 5 columns
//        Sort sortOrder = getSort(sort);
//        PageRequest pageable = PageRequest.of(page, size, sortOrder);
//
//        model.addAttribute("newestProductPage", productService.findProductSaleDataPaginated(pageable));
//        model.addAttribute("categories", categoryService.findAll());
//        model.addAttribute("isHomePage", false);
//        model.addAttribute("cartItemCount", getCurrentCartItemCount());
//        model.addAttribute("currentSort", sort);
//        
//        return "product/new-products";
//    }
//    
//    // === SẢN PHẨM BÁN CHẠY ===
//    @GetMapping("/products/best-selling")
//    public String showBestSellingProductsPage(
//            @RequestParam(name = "page", defaultValue = "0") int page,
//            @RequestParam(name = "sort", required = false, defaultValue = "bestSelling") String sort,
//            Model model) {
//        
//        int size = 2;
//        Sort sortOrder = getSort(sort);
//        PageRequest pageable = PageRequest.of(page, size, sortOrder);
//
//        model.addAttribute("bestSellingProductPage", productService.findProductSaleDataPaginated(pageable));
//        model.addAttribute("categories", categoryService.findAll());
//        model.addAttribute("isHomePage", false);
//        model.addAttribute("cartItemCount", getCurrentCartItemCount());
//        model.addAttribute("currentSort", sort);
//
//        return "product/best-selling-products";
//    }
//    
//    // === SẢN PHẨM ĐÁNH GIÁ CAO ===
//    @GetMapping("/products/top-rated")
//    public String showTopRatedProductsPage(
//            @RequestParam(name = "page", defaultValue = "0") int page,
//            @RequestParam(name = "sort", required = false, defaultValue = "topRated") String sort,
//            Model model) {
//        
//        int size = 2;
//        Sort sortOrder = getSort(sort);
//        PageRequest pageable = PageRequest.of(page, size, sortOrder);
//
//        model.addAttribute("topRatedProductPage", productService.findProductSaleDataPaginated(pageable));
//        model.addAttribute("categories", categoryService.findAll());
//        model.addAttribute("isHomePage", false);
//        model.addAttribute("cartItemCount", getCurrentCartItemCount());
//        model.addAttribute("currentSort", sort);
//
//        return "product/top-rated-products";
//    }
//    
//    // === SẢN PHẨM YÊU THÍCH ===
//    @GetMapping("/products/top-liked")
//    public String showTopLikedProductsPage(
//            @RequestParam(name = "page", defaultValue = "0") int page,
//            @RequestParam(name = "sort", required = false, defaultValue = "topLiked") String sort,
//            Model model) {
//        
//        int size = 2;
//        Sort sortOrder = getSort(sort);
//        PageRequest pageable = PageRequest.of(page, size, sortOrder);
//
//        model.addAttribute("topLikedProductPage", productService.findProductSaleDataPaginated(pageable));
//        model.addAttribute("categories", categoryService.findAll());
//        model.addAttribute("isHomePage", false);
//        model.addAttribute("cartItemCount", getCurrentCartItemCount());
//        model.addAttribute("currentSort", sort);
//
//        return "product/top-liked-products";
//    }
//    
//    // === TRANG DANH MỤC ===
//    @GetMapping("/categories/{categoryId}")
//    public String showCategoryProductsPage(
//            @PathVariable("categoryId") Integer categoryId,
//            @RequestParam(name = "page", defaultValue = "0") int page,
//            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
//            Model model) {
//
//        Category category = categoryService.findById(categoryId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
//
//        Sort sortOrder = getSort(sort);
//        int size = 1;
//        Pageable pageable = PageRequest.of(page, size, sortOrder);
//        Page<ProductSaleDTO> productPage = productService.findProductSaleDataByCategoryPaginated(category, pageable);
//
//        model.addAttribute("productPage", productPage);
//        model.addAttribute("currentCategory", category);
//        model.addAttribute("categories", categoryService.findAll());
//        model.addAttribute("isHomePage", false);
//        model.addAttribute("currentSort", sort);
//        model.addAttribute("categoryId", categoryId); // Thêm để pagination biết
//        model.addAttribute("cartItemCount", getCurrentCartItemCount());
//
//        return "product/category-products";
//    }
//    
//    /**
//     * Helper method để tạo Sort object
//     */
//    private Sort getSort(String sortType) {
//        switch (sortType) {
//            case "priceAsc":
//                return Sort.by(Sort.Direction.ASC, "basePrice");
//            case "priceDesc":
//                return Sort.by(Sort.Direction.DESC, "basePrice");
//            case "nameAsc":
//                return Sort.by(Sort.Direction.ASC, "productName");
//            case "nameDesc":
//                return Sort.by(Sort.Direction.DESC, "productName");
//            case "bestSelling":
//                return Sort.by(Sort.Direction.DESC, "soldCount");
//            case "topRated":
//                return Sort.by(Sort.Direction.DESC, "averageRating")
//                           .and(Sort.by(Sort.Direction.DESC, "totalReviews"));
//            case "topLiked":
//                return Sort.by(Sort.Direction.DESC, "totalLikes");
//            case "newest":
//            default:
//                return Sort.by(Sort.Direction.DESC, "createdAt");
//        }
//    }
//}

package com.alotra.controller; // Giữ package này

// Import các entity đã merge
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.user.User; // Sử dụng User

// Import Service và Repository đã merge
import com.alotra.model.ProductSaleDTO; // Giữ DTO này nếu ProductRepository còn dùng
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;
// import com.alotra.service.user.CustomerService; // *** BỎ CustomerService ***
import com.alotra.service.user.UserService; // Sử dụng UserService

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Qualifier; // Bỏ nếu không cần
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Thêm exception
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

    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private CartService cartService;
    // @Autowired @Qualifier("userServiceImpl") // Qualifier có thể không cần
    @Autowired private UserService userService; // *** GIỮ UserService ***
    // @Autowired private CustomerService customerService; // *** BỎ CustomerService ***

    // Inner class Banner giữ nguyên
    public static class Banner {
        private String imageUrl;
        private String altText;
        public Banner(String u, String a) { this.imageUrl = u; this.altText = a; }
        public String getImageUrl() { return imageUrl; }
        public String getAltText() { return altText; }
    }

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

    @GetMapping("/")
    public String home(Model model, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", true);
        model.addAttribute("cartItemCount", getCurrentCartItemCount()); // Đã sửa

        List<Banner> banners = new ArrayList<>();
        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/09/cover-web-khe%CC%82%CC%81-scaled.jpg", "Banner PhinDi"));
        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/08/cover-web-warabi-scaled.jpg", "Banner Trà"));
        model.addAttribute("banners", banners);

        // Sort definitions (giữ nguyên, nhưng kiểm tra Product entity đã merge có các field này không)
        // Lưu ý: topLikedSort dùng 'totalLikes' - trường này có thể không còn tồn tại sau merge
        Sort topSellingSort = Sort.by(Sort.Direction.DESC, "soldCount");
        Sort newestSort = Sort.by(Sort.Direction.DESC, "createdAt");
        Sort topRatedSort = Sort.by(Sort.Direction.DESC, "averageRating")
                                .and(Sort.by(Sort.Direction.DESC, "totalReviews"));
        // Sort topLikedSort = Sort.by(Sort.Direction.DESC, "totalLikes"); // *** XEM XÉT LẠI FIELD NÀY ***

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated); // Thêm biến này cho view tiện kiểm tra

        if (isAuthenticated) {
            // Hiển thị nhiều loại sản phẩm hơn cho người dùng đã đăng nhập
            model.addAttribute("newestProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, newestSort)).getContent());
            model.addAttribute("topProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topSellingSort)).getContent());
            model.addAttribute("topRatedProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topRatedSort)).getContent());
            // model.addAttribute("topLikedProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topLikedSort)).getContent()); // *** XEM XÉT LẠI ***
        }
        else {
            // Chỉ hiển thị bán chạy cho khách vãng lai
            model.addAttribute("topProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 20, topSellingSort)).getContent());
        }

        return "home/index"; // View index.html
    }

    // === CÁC TRANG SẢN PHẨM KHÁC (ví dụ: new, best-selling, top-rated, top-liked) ===
    // Các phương thức này giữ nguyên logic phân trang và sắp xếp,
    // chỉ cần đảm bảo getCurrentCartItemCount() hoạt động đúng.

    // === SẢN PHẨM MỚI ===
    @GetMapping("/products/new")
    public String showNewProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            Model model) {

        int size = 15; // Ví dụ: 3 rows x 5 columns
        Sort sortOrder = getSort(sort); // Hàm helper getSort vẫn dùng được
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        // Giả sử ProductService trả về Page<ProductSaleDTO>
        model.addAttribute("productPage", productService.findProductSaleDataPaginated(pageable)); // Đổi tên attribute cho rõ ràng
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount()); // Đã sửa
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/new"); // Thêm path cho pagination

        return "product/product-list"; // Dùng chung view product-list.html
    }

    // === SẢN PHẨM BÁN CHẠY ===
    @GetMapping("/products/best-selling")
    public String showBestSellingProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "bestSelling") String sort,
            Model model) {

        int size = 15;
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        model.addAttribute("productPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/best-selling"); // Thêm path cho pagination

        return "product/product-list"; // Dùng chung view product-list.html
    }

    // === SẢN PHẨM ĐÁNH GIÁ CAO ===
    @GetMapping("/products/top-rated")
    public String showTopRatedProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "topRated") String sort,
            Model model) {

        int size = 15;
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        model.addAttribute("productPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/top-rated"); // Thêm path cho pagination

        return "product/product-list"; // Dùng chung view product-list.html
    }

    // === SẢN PHẨM YÊU THÍCH (NẾU GIỮ LẠI FIELD totalLikes) ===
    // *** CẦN KIỂM TRA LẠI FIELD totalLikes TRONG Product.java ĐÃ MERGE ***
    @GetMapping("/products/top-liked")
    public String showTopLikedProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "topLiked") String sort,
            Model model) {

        // Kiểm tra xem trường totalLikes có còn tồn tại không
        // Nếu không, cần bỏ endpoint này hoặc sửa logic sort
        try {
             Product.class.getDeclaredField("totalLikes"); // Thử truy cập field
        } catch (NoSuchFieldException e) {
             // Nếu field không tồn tại, redirect hoặc hiển thị lỗi
             // Ví dụ: return "redirect:/?error=feature_unavailable";
             System.err.println("WARN: Field 'totalLikes' not found in Product entity. Top Liked feature might be broken.");
             // Hoặc sắp xếp theo tiêu chí khác, ví dụ newest
             sort = "newest";
        }


        int size = 15;
        Sort sortOrder = getSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        model.addAttribute("productPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/top-liked"); // Thêm path cho pagination

        return "product/product-list"; // Dùng chung view product-list.html
    }

    // === TRANG DANH MỤC ===
    @GetMapping("/categories/{categoryId}")
    public String showCategoryProductsPage(
            @PathVariable("categoryId") Integer categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            Model model) {
        try {
            Category category = categoryService.findById(categoryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));

            Sort sortOrder = getSort(sort);
            int size = 15; // Số sản phẩm mỗi trang danh mục
            Pageable pageable = PageRequest.of(page, size, sortOrder);
            // Giả sử ProductService có phương thức này
            Page<ProductSaleDTO> productPage = productService.findProductSaleDataByCategoryPaginated(category, pageable);

            model.addAttribute("productPage", productPage);
            model.addAttribute("currentCategory", category);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("isHomePage", false);
            model.addAttribute("currentSort", sort);
            model.addAttribute("categoryId", categoryId); // Giữ categoryId cho pagination
            model.addAttribute("cartItemCount", getCurrentCartItemCount()); // Đã sửa
            model.addAttribute("pagePath", "/categories/" + categoryId); // Thêm path cho pagination

            return "product/product-list"; // Dùng chung view product-list.html

        } catch (ResponseStatusException e) {
            // Xử lý lỗi không tìm thấy category (ví dụ: redirect về home)
             return "redirect:/?error=category_not_found";
        }
    }

    // === Helper method để tạo Sort object (Giữ nguyên) ===
    private Sort getSort(String sortType) {
        if (sortType == null) sortType = "newest"; // Đảm bảo không null
        switch (sortType) {
            case "priceAsc":
                // *** KIỂM TRA LẠI FIELD basePrice TRONG Product.java ĐÃ MERGE ***
                return Sort.by(Sort.Direction.ASC, "basePrice"); // Nếu basePrice bị xóa, cần sửa thành price từ variant?
            case "priceDesc":
                // *** KIỂM TRA LẠI FIELD basePrice TRONG Product.java ĐÃ MERGE ***
                return Sort.by(Sort.Direction.DESC, "basePrice"); // Nếu basePrice bị xóa, cần sửa thành price từ variant?
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
                // *** KIỂM TRA LẠI FIELD totalLikes TRONG Product.java ĐÃ MERGE ***
                return Sort.by(Sort.Direction.DESC, "totalLikes"); // Nếu totalLikes bị xóa, cần sửa
            case "newest":
            default:
                return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }
}