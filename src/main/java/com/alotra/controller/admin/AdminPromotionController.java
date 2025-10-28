package com.alotra.controller.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.promotion.PromotionProduct;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.product.ProductService;
import com.alotra.service.promotion.PromotionProductService;
import com.alotra.service.promotion.PromotionService;
import com.alotra.service.shop.ShopService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("admin/promotions")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminPromotionController {
	
	@Autowired
	PromotionService promotionService;
	
	@Autowired
	PromotionProductService promotionProductService;
	
	@Autowired
    ShopRepository shopRepository;
	
	@Autowired
    ProductRepository productRepository;
	
	@Autowired
	ShopService shopService;
	
	@Autowired
	ProductService productService;
	
	@Autowired
	UserRepository userRepository;
	
	@GetMapping("")
    public String listPromotions(Model model) {
        List<Promotion> promotions = promotionService.getAllPromotions(); 

        
        model.addAttribute("promotions", promotions);
        return "admin/promotions/list"; 
    }
	
	@GetMapping("/detail/{id}")
	public String detailPromotion(@PathVariable("id") Integer id, ModelMap model) {
		Promotion promotion = promotionService.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy id: " + id));
		
		List<PromotionProduct> promotionProducts = promotionProductService.findByPromotionId(id);
        model.addAttribute("promotionProducts", promotionProducts);

		model.addAttribute("promotion", promotion);
		model.addAttribute("activeMenu", "promotions");

		return "admin/promotions/detail";
	}
	
	@GetMapping("/add")
    public String showCreateForm(Model model) {
        model.addAttribute("promotion", new Promotion());

        model.addAttribute("allShops", shopService.findAllActive());
        model.addAttribute("allProducts", productService.findAllActive());
        
        return "admin/promotions/add"; 
    }

	@PostMapping("/save")
	public String savePromotion(
	        @Valid @ModelAttribute Promotion promotion,
	        BindingResult bindingResult,
	        @RequestParam(required = false) List<Integer> applicableShopIds,
	        @RequestParam(required = false) List<Integer> applicableProductIds,
	        RedirectAttributes redirectAttributes,
	        Model model
	) {
	    // Nếu có lỗi validate
	    if (bindingResult.hasErrors()) {
	        model.addAttribute("allShops", shopService.findAllActive());
	        model.addAttribute("allProducts", productService.findAllActive());
	        return "admin/promotions/add";
	    }

	    try {
	        // Kiểm tra ngày kết thúc sau ngày bắt đầu
	        if (promotion.getEndDate().isBefore(promotion.getStartDate())) {
	            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
	        }

	        // Kiểm tra mã khuyến mãi tồn tại
	        if (promotionService.existsByPromoCode(promotion.getPromoCode())) {
	            throw new IllegalArgumentException("Mã khuyến mãi đã tồn tại: " + promotion.getPromoCode());
	        }

	        User admin = userRepository.findById(1).orElseThrow(() -> new RuntimeException("Admin not found"));
	        promotion.setCreatedByUserID(admin);
	       

	        // Lưu promotion
	        Promotion savedPromotion = promotionService.save(promotion);

	        // Gom tất cả productIds
	        Set<Integer> allProductIds = new HashSet<>();

	        if (applicableShopIds != null && !applicableShopIds.isEmpty()) {
	            List<Integer> productsFromShops = productService.findProductIdsByShopIds(applicableShopIds);
	            allProductIds.addAll(productsFromShops);
	        }

	        if (applicableProductIds != null && !applicableProductIds.isEmpty()) {
	            allProductIds.addAll(applicableProductIds);
	        }

	        // Lưu các PromotionProduct (chỉ lưu entity, không tính toán gì)
	        if (!allProductIds.isEmpty()) {
	            promotionService.savePromotionProducts(
	                    savedPromotion.getPromotionId(),
	                    new ArrayList<>(allProductIds)
	            );
	        }

	        // Thông báo thành công
	        redirectAttributes.addFlashAttribute("successMessage",
	                "Thêm khuyến mãi thành công: " + promotion.getPromotionName() +
	                        " (Áp dụng cho " + allProductIds.size() + " sản phẩm)"
	        );
	        return "redirect:/admin/promotions";

	    } catch (IllegalArgumentException e) {
	        model.addAttribute("errorMessage", e.getMessage());
	        model.addAttribute("allShops", shopService.findAllActive());
	        model.addAttribute("allProducts", productService.findAllActive());
	        return "admin/promotions/add";

	    } catch (Exception e) {
	        model.addAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
	        model.addAttribute("allShops", shopService.findAllActive());
	        model.addAttribute("allProducts", productService.findAllActive());
	        return "admin/promotions/add";
	    }
	}

	@PostMapping("/deactivate/{id}")
	public String deactivatePromotion(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
	    try {
	        Promotion promotion = promotionService.findById(id)
	                .orElseThrow(() -> new RuntimeException("Promotion not found"));

	        promotion.setStatus((byte) 0); 
	        promotionService.save(promotion);

	        redirectAttributes.addFlashAttribute("successMessage", 
	            "Hủy kích hoạt khuyến mãi thành công: " + promotion.getPromotionName());
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("errorMessage", 
	            "Có lỗi xảy ra khi hủy khuyến mãi: " + e.getMessage());
	    }

	    return "redirect:/admin/promotions";
	}

}
