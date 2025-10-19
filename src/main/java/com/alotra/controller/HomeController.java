package com.alotra.controller;

import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    // Class Banner (giữ nguyên)
    public static class Banner {
        private String imageUrl;
        private String altText;
        public Banner(String u, String a) { this.imageUrl = u; this.altText = a; }
        public String getImageUrl() { return imageUrl; }
        public String getAltText() { return altText; }
    }

    @GetMapping("/")
    public String home(Model model, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", true);

        // Thêm dữ liệu banner (giữ nguyên)
        List<Banner> banners = new ArrayList<>();
        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/09/cover-web-khe%CC%82%CC%81-scaled.jpg", "Banner PhinDi"));
        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/08/cover-web-warabi-scaled.jpg", "Banner Trà"));
        model.addAttribute("banners", banners);
        
        // === TẠO CÁC ĐỐI TƯỢNG SORT ===
        // Sắp xếp Bán chạy (dựa trên cột 'soldCount' đã cache của Product)
        Sort topSellingSort = Sort.by(Sort.Direction.DESC, "soldCount");
        
        // Sắp xếp Sản phẩm mới
        Sort newestSort = Sort.by(Sort.Direction.DESC, "createdAt");
        
        // Sắp xếp Đánh giá cao
        Sort topRatedSort = Sort.by(Sort.Direction.DESC, "averageRating")
                                .and(Sort.by(Sort.Direction.DESC, "totalReviews"));
                                
        // Sắp xếp Yêu thích (dựa trên cột 'totalLikes' mới)
        Sort topLikedSort = Sort.by(Sort.Direction.DESC, "totalLikes");


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            
            // === GỌI HÀM SERVICE CHUNG ===
            model.addAttribute("newestProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, newestSort)).getContent());
            model.addAttribute("topProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topSellingSort)).getContent());
            model.addAttribute("topRatedProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topRatedSort)).getContent());
            model.addAttribute("topLikedProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 10, topLikedSort)).getContent());
            
        }
        else {
            // Người dùng chưa đăng nhập, chỉ hiển thị 20 sản phẩm bán chạy
            model.addAttribute("topProducts", productService.findProductSaleDataPaginated(PageRequest.of(0, 20, topSellingSort)).getContent());
        }
        	
        return "home/index";
    }

    @GetMapping("/products/new")
    public String showNewProductsPage(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        int size = 2; // Tăng size cho trang "Xem tất cả"
        
        // Sắp xếp theo sản phẩm mới
        Sort newestSort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageable = PageRequest.of(page, size, newestSort);

        model.addAttribute("newestProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);

        return "product/new-products";
    }
    
    @GetMapping("/products/best-selling")
    public String showBestSellingProductsPage(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        int size = 2; // Tăng size
        
        // Sắp xếp theo bán chạy
        Sort topSellingSort = Sort.by(Sort.Direction.DESC, "soldCount");
        PageRequest pageable = PageRequest.of(page, size, topSellingSort);

        model.addAttribute("bestSellingProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);

        return "product/best-selling-products";
    }
    
    @GetMapping("/products/top-rated")
    public String showTopRatedProductsPage(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        int size = 2; // Tăng size
        
        // Sắp xếp theo đánh giá cao
        Sort topRatedSort = Sort.by(Sort.Direction.DESC, "averageRating")
                                .and(Sort.by(Sort.Direction.DESC, "totalReviews"));
        
        PageRequest pageable = PageRequest.of(page, size, topRatedSort);

        model.addAttribute("topRatedProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);

        return "product/top-rated-products";
    }
    
    // === THÊM HÀM MỚI CHO TOP YÊU THÍCH ===
    @GetMapping("/products/top-liked")
    public String showTopLikedProductsPage(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        int size = 2;
        
        // Sắp xếp theo yêu thích
        Sort topLikedSort = Sort.by(Sort.Direction.DESC, "totalLikes");
        PageRequest pageable = PageRequest.of(page, size, topLikedSort);

        model.addAttribute("topLikedProductPage", productService.findProductSaleDataPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);

        return "product/top-liked-products";
    }
}