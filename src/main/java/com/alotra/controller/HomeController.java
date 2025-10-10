package com.alotra.controller;

import com.alotra.service.product.CategoryService;
// Không cần ProductService ở đây nữa nếu bạn hiển thị theo category
// import com.alotra.service.product.ProductService; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    // Bỏ ProductService vì chúng ta đã lấy Product thông qua Category
    // @Autowired
    // private ProductService productService;

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
    public String home(Model model) {
        // === SỬA LỖI Ở ĐÂY ===
        // 1. Lấy danh sách categories (đã bao gồm products bên trong) từ service
        model.addAttribute("categories", categoryService.findAll());

        // 2. Dòng này không cần thiết nữa vì ta đã lặp qua category.products
        // model.addAttribute("topProducts", productService.getTopProducts());

        // 3. Giữ lại dữ liệu mẫu cho banner
        List<Banner> banners = new ArrayList<>();
        banners.add(new Banner("https://bizweb.dktcdn.net/100/487/455/themes/917232/assets/slider_3.jpg?1759892738511", "Banner PhinDi"));
        banners.add(new Banner("https://bizweb.dktcdn.net/100/487/455/themes/917232/assets/slider_1.jpg?1759892738511", "Banner Trà"));
        model.addAttribute("banners", banners);

        // Đảm bảo trả về đúng tên template "index"
        return "home/index";
    }
}