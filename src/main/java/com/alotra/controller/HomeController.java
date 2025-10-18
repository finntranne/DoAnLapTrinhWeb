package com.alotra.controller;

import com.alotra.entity.product.Product;
import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    // Class Banner cho carousel
    public static class Banner {
        private String imageUrl;
        private String altText;
        public Banner(String u, String a) { this.imageUrl = u; this.altText = a; }
        public String getImageUrl() { return imageUrl; }
        public String getAltText() { return altText; }
    }

    @GetMapping("/")
    public String home(Model model, @RequestParam(defaultValue = "0") int page) {
        // Thêm danh sách categories
        model.addAttribute("categories", categoryService.findAll());
        
        // Thêm flag để biết đây là trang chủ
        model.addAttribute("isHomePage", true);

        // Thêm dữ liệu banner
        List<Banner> banners = new ArrayList<>();
        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/09/cover-web-khe%CC%82%CC%81-scaled.jpg", "Banner PhinDi"));
        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/08/cover-web-warabi-scaled.jpg", "Banner Trà"));
        model.addAttribute("banners", banners);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            model.addAttribute("newestProducts", productService.findNewestProductsPaginated(PageRequest.of(0, 10)));
            model.addAttribute("topProducts", productService.getBestSellingProductsPaginated(PageRequest.of(0, 10)));
        }
        else {
        	// Thêm danh sách top products
            model.addAttribute("topProducts", productService.getBestSellingProductsPaginated(PageRequest.of(0, 20)));
        }
        	

        // Trả về template home/index
        return "home/index";
    }

    @GetMapping("/products/new")
    public String showNewProductsPage(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        int size = 2;
        PageRequest pageable = PageRequest.of(page, size);

        model.addAttribute("newestProductPage", productService.findNewestProductsPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        
        // Đánh dấu đây KHÔNG phải trang chủ
        model.addAttribute("isHomePage", false);

        return "product/new-products";
    }
    
    @GetMapping("/products/best-selling")
    public String showBestSellingProductsPage(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        int size = 1;
        PageRequest pageable = PageRequest.of(page, size);

        model.addAttribute("bestSellingProductPage", productService.getBestSellingProductsPaginated(pageable));
        model.addAttribute("categories", categoryService.findAll());
        
        // Đánh dấu đây KHÔNG phải trang chủ
        model.addAttribute("isHomePage", false);

        return "product/best-selling-products";
    }
}