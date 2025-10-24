package com.alotra.controller;

import com.alotra.entity.product.Product;
import com.alotra.entity.product.Topping;
import com.alotra.entity.user.Customer;
import com.alotra.entity.user.Review;
import com.alotra.entity.user.User;
import com.alotra.model.ProductSaleDTO;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;
import com.alotra.service.user.CustomerService;
import com.alotra.service.user.ReviewService;
import com.alotra.service.user.UserService;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private CategoryService categoryService; // Cần cho breadcrumb

    @Autowired
    private ReviewService reviewService; // Cần cho đánh giá
    
    @Autowired private CartService cartService;
    
    @Autowired @Qualifier("userServiceImpl") private UserService userService; // Giữ nguyên
    
    @Autowired private CustomerService customerService; // Giữ nguyên
    
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
        
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        
     // --- PASS TOPPINGS TO VIEW ---
        // Filter only active toppings if needed
        Set<Topping> activeToppings = product.getAvailableToppings().stream()
                                          .filter(t -> t.getStatus() != null && t.getStatus() == 1)
                                          .collect(Collectors.toSet());
        model.addAttribute("toppings", activeToppings);
        // -----------------------------
        
        // Cần cho layout
        model.addAttribute("categories", categoryService.findAll()); 
        model.addAttribute("isHomePage", false);

        return "product/detail"; // Trả về file view mới
    }
    
    @GetMapping("/search")
    public String searchProducts(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page, // ⚠️ ĐỔI THÀNH 0-indexed
            @RequestParam(name = "sort", required = false, defaultValue = "priceDesc") String sort,
            Model model) {

        try {
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
        } catch (Exception e) { /* Handle or ignore */ }

        int pageSize = 2; // Recommended: 3 rows x 5 columns

        // === CREATE SORT OBJECT ===
        Sort sortOrder;
        switch (sort) {
            case "nameAsc":
                sortOrder = Sort.by("productName").ascending(); 
                break;
            case "nameDesc":
                sortOrder = Sort.by("productName").descending();
                break;
            case "priceAsc":
                sortOrder = Sort.by("basePrice").ascending(); 
                break;
            case "priceDesc":
            default:
                sortOrder = Sort.by("basePrice").descending(); 
                sort = "priceDesc"; 
                break;
        }

        Pageable pageable = PageRequest.of(page, pageSize, sortOrder); // page đã là 0-indexed

        Page<ProductSaleDTO> salePage = productService.findProductSaleDataByKeyword(keyword, pageable);

        // Pass data to View
        model.addAttribute("sales", salePage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentSort", sort);
        model.addAttribute("totalItems", salePage.getTotalElements());
        model.addAttribute("isHomePage", false);

        return "shop/search_results";
    }
}