package com.alotra.controller;

import com.alotra.entity.product.Product;
import com.alotra.entity.user.Review;
import com.alotra.model.ProductSaleDTO;
import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;
import com.alotra.service.user.ReviewService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private CategoryService categoryService; // Cần cho breadcrumb

    @Autowired
    private ReviewService reviewService; // Cần cho đánh giá

    @GetMapping("/products/{id}")
    public String getProductDetail(@PathVariable("id") Integer id, Model model) {
        
        // 1. Lấy DTO chính của sản phẩm
        ProductSaleDTO saleDTO = productService.findProductSaleDataById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        
        Product product = saleDTO.getProduct();
        
        // 2. Lấy danh sách đánh giá (ví dụ: 5 đánh giá đầu tiên)
        Page<Review> reviewPage = reviewService.findByProduct(product, PageRequest.of(0, 5));
        
        // 3. Lấy sản phẩm liên quan (cùng danh mục, 5 sản phẩm)
        Page<ProductSaleDTO> relatedProducts = productService.findProductSaleDataByCategoryPaginated(
            product.getCategory(), 
            PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "soldCount"))
        );

        // 4. Đưa tất cả vào model
        model.addAttribute("saleDTO", saleDTO);
        model.addAttribute("product", product);
        model.addAttribute("variants", product.getProductVariants()); // Cho JS chọn size
        model.addAttribute("reviews", reviewPage.getContent());
        model.addAttribute("relatedProducts", relatedProducts.getContent());
        
        // Cần cho layout
        model.addAttribute("categories", categoryService.findAll()); 
        model.addAttribute("isHomePage", false);

        return "product/detail"; // Trả về file view mới
    }
}