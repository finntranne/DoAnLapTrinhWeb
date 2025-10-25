package com.alotra.controller.admin;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.promotion.Promotion;
import com.alotra.execption.InvalidPromotionException;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.service.promotion.PromotionService;

@Controller
@RequestMapping("admin/promotions")
public class PromotionController {
	
	@Autowired
	PromotionService promotionService;
	
	@Autowired
    ShopRepository shopRepository;
	
	@Autowired
    ProductRepository productRepository;
	
	@GetMapping("")
    public String listPromotions(Model model) {
        List<Promotion> promotions = promotionService.getAllPromotions(); 
        
        model.addAttribute("promotions", promotions);
        return "promotions/list"; 
    }
	
	@GetMapping("/add")
    public String showCreateForm(Model model) {
        model.addAttribute("promotion", new Promotion());
        model.addAttribute("allShops", shopRepository.findAll());
        model.addAttribute("allProducts", productRepository.findAll());
        
        return "promotions/add"; 
    }
	
	@PostMapping("/save")
    public String savePromotion(
            @ModelAttribute("promotion") Promotion promotion,
            @RequestParam(name = "applicableShopIds", required = false) Set<Integer> applicableShopIds,
            @RequestParam(name = "applicableProductIds", required = false) Set<Integer> applicableProductIds,
            RedirectAttributes redirectAttributes,
            Model model) { 
                
        try {

            promotionService.createPromotion(
                promotion, 
                null,
                applicableShopIds, 
                applicableProductIds
            );
            
            redirectAttributes.addFlashAttribute("successMessage", "Tạo khuyến mãi thành công!");
            return "redirect:/admin/promotions"; // Chuyển hướng về trang danh sách

        } catch (InvalidPromotionException e) {
            // Nếu LỖI (ví dụ: trùng code, ngày sai), ở lại form và báo lỗi
            model.addAttribute("errorMessage", e.getMessage());
            
            // QUAN TRỌNG: Phải gửi lại danh sách shops/products
            // vì trang được render lại
            model.addAttribute("allShops", shopRepository.findAll());
            model.addAttribute("allProducts", productRepository.findAll());
            
            // Ở lại file: templates/admin/promotion-form.html
            return "/promotion/add"; 
        }
    }

}
