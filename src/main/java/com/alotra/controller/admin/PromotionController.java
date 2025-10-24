package com.alotra.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alotra.entity.promotion.Promotion;
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
        // Giả sử bạn thêm hàm getAllPromotions() vào Service
        // List<Promotion> promotions = promotionService.getAllPromotions();
        
        // Hoặc tạm dùng:
        List<Promotion> promotions = promotionService.getAllPromotions(); 
        
        model.addAttribute("promotions", promotions);
        return "promotions/list"; // Trả về file: templates/admin/promotion-list.html
    }

}
