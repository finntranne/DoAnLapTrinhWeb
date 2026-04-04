package com.alotra.controller;

import com.alotra.entity.product.Category;
import com.alotra.entity.user.User;
import com.alotra.model.ProductSaleDTO;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;
import com.alotra.service.shop.StoreService;
import com.alotra.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CartService cartService;
    @Autowired
    private UserService userService;
    @Autowired
    private StoreService storeService;

    public static class Banner {
        private final String imageUrl;
        private final String altText;

        public Banner(String imageUrl, String altText) {
            this.imageUrl = imageUrl;
            this.altText = altText;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getAltText() {
            return altText;
        }
    }

    private int getCurrentCartItemCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            try {
                User user = userService.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found during cart count"));
                return cartService.getCartItemCount(user);
            } catch (UsernameNotFoundException e) {
                return 0;
            }
        }
        return 0;
    }

    private Integer getSelectedShopId(HttpSession session) {
        Integer selectedShopId = (Integer) session.getAttribute("selectedShopId");
        return selectedShopId == null ? 0 : selectedShopId;
    }

    @PostMapping("/select-shop/{shopId}")
    public ResponseEntity<?> selectShop(@PathVariable Integer shopId, HttpSession session) {
        session.setAttribute("selectedShopId", shopId);
        session.setAttribute("selectedShopName", storeService.getShopNameById(shopId));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/")
    public String home(Model model, @RequestParam(defaultValue = "0") int page, HttpSession session) {
        Integer selectedShopId = getSelectedShopId(session);

        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", true);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

        List<Banner> banners = new ArrayList<>();
        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/09/cover-web-khe%CC%82%CC%81-scaled.jpg",
                "Banner PhinDi"));
        banners.add(new Banner("https://gongcha.com.vn/wp-content/uploads/2025/08/cover-web-warabi-scaled.jpg",
                "Banner Tra"));
        model.addAttribute("banners", banners);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        model.addAttribute("newestProducts",
                productService.findProductSaleDataSorted(selectedShopId, 0, 10, "newest").getContent());
        model.addAttribute("topProducts",
                productService.findProductSaleDataSorted(selectedShopId, 0, 10, "bestSelling").getContent());
        model.addAttribute("topRatedProducts",
                productService.findProductSaleDataSorted(selectedShopId, 0, 10, "topRated").getContent());
        model.addAttribute("topLikedProducts",
                productService.findProductSaleDataSorted(selectedShopId, 0, 10, "topLiked").getContent());

        return "home/index";
    }

    @GetMapping("/products/new")
    public String showNewProductsPage(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            Model model, HttpSession session) {
        Integer selectedShopId = getSelectedShopId(session);

        model.addAttribute("newestProductPage",
                productService.findProductSaleDataSorted(selectedShopId, page, 2, sort));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/new");
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
        model.addAttribute("shopId", selectedShopId);

        return "product/new-products";
    }

    @GetMapping("/products/best-selling")
    public String showBestSellingProductsPage(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "bestSelling") String sort,
            Model model, HttpSession session) {
        Integer selectedShopId = getSelectedShopId(session);

        model.addAttribute("bestSellingProductPage",
                productService.findProductSaleDataSorted(selectedShopId, page, 2, sort));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/best-selling");
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
        model.addAttribute("shopId", selectedShopId);

        return "product/best-selling-products";
    }

    @GetMapping("/products/top-rated")
    public String showTopRatedProductsPage(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "topRated") String sort,
            Model model, HttpSession session) {
        Integer selectedShopId = getSelectedShopId(session);

        model.addAttribute("topRatedProductPage",
                productService.findProductSaleDataSorted(selectedShopId, page, 2, sort));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/top-rated");
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
        model.addAttribute("shopId", selectedShopId);

        return "product/top-rated-products";
    }

    @GetMapping("/products/top-liked")
    public String showTopLikedProductsPage(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "topLiked") String sort,
            Model model, HttpSession session) {
        Integer selectedShopId = getSelectedShopId(session);

        model.addAttribute("topLikedProductPage",
                productService.findProductSaleDataSorted(selectedShopId, page, 2, sort));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isHomePage", false);
        model.addAttribute("cartItemCount", getCurrentCartItemCount());
        model.addAttribute("currentSort", sort);
        model.addAttribute("pagePath", "/products/top-liked");
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
        model.addAttribute("shopId", selectedShopId);

        return "product/top-liked-products";
    }

    @GetMapping("/categories/{categoryId}")
    public String showCategoryProductsPage(@PathVariable("categoryId") Integer categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            Model model, HttpSession session) {
        try {
            Integer selectedShopId = getSelectedShopId(session);
            Category category = categoryService.findById(categoryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay danh muc"));

            Page<ProductSaleDTO> productPage = productService.findProductSaleDataByCategorySorted(category,
                    selectedShopId, page, 1, sort);

            model.addAttribute("productPage", productPage);
            model.addAttribute("currentCategory", category);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("isHomePage", false);
            model.addAttribute("currentSort", sort);
            model.addAttribute("categoryId", categoryId);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("pagePath", "/categories/" + categoryId);
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
            model.addAttribute("shopId", selectedShopId);

            return "product/category-products";
        } catch (ResponseStatusException e) {
            return "redirect:/?error=category_not_found";
        }
    }
}
