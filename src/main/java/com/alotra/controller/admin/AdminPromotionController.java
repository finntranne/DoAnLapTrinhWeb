package com.alotra.controller.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

import com.alotra.entity.product.ProductApproval;
import com.alotra.entity.product.Topping;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.promotion.PromotionApproval;
import com.alotra.entity.promotion.PromotionProduct;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.product.ProductApprovalService;
import com.alotra.service.product.ProductService;
import com.alotra.service.promotion.PromotionApprovalService;
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
	
	@Autowired
	PromotionApprovalService promotionApprovalService;
	
	@GetMapping({""})
    public String listPromotions(Model model) {
        List<Promotion> promotions = promotionService.getAllPromotionsApproval(); 

        
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
	
	@GetMapping("/pending")
	public String listPending(ModelMap model, @RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "10") int size) {

		int actualPage = Math.max(1, page);
		Pageable pageable = PageRequest.of(actualPage - 1, size, Sort.by("requestedAt").descending());
		Page<PromotionApproval> approvalPage = promotionApprovalService.findByStatus("PENDING", pageable);
		System.out.print(approvalPage);

		int totalPages = approvalPage.getTotalPages();

		model.addAttribute("promotions", approvalPage.getContent());
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", totalPages);

		model.addAttribute("activeMenu", "promotions");

		return "admin/promotions/pending";
	}
	
	@GetMapping("/pending/detail/{id}")
	public String showPendingApprovalDetail(@PathVariable("id") Integer approvalId, ModelMap model) {

		Optional<PromotionApproval> approvalOpt = promotionApprovalService.findById(approvalId);

		if (approvalOpt.isEmpty()) {
			model.addAttribute("errorMessage", "Không tìm thấy yêu cầu phê duyệt có ID: " + approvalId);
			return "error/404";
		}

		

		PromotionApproval approval = approvalOpt.get();

		model.addAttribute("approval", approval);
		

		model.addAttribute("activeMenu", "promotions");
		return "admin/promotions/approval-detail";
	}
	
	@PostMapping("/approve/{id}")
	public String approveProduct(@PathVariable("id") Integer approvalId, RedirectAttributes redirectAttributes,
			Authentication authentication) {


		Integer reviewedByUserId = 1;
		

		try {
			promotionApprovalService.approveProductChange(approvalId, reviewedByUserId);
			redirectAttributes.addFlashAttribute("success", "Phê duyệt yêu cầu #" + approvalId + " thành công!");
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("error", "Phê duyệt thất bại. Chi tiết: " + e.getMessage());
		}

		return "redirect:/admin/promotions/pending";

	}
	
	@PostMapping("/reject/{id}")
	public String rejectProduct(@PathVariable("id") Integer approvalId,
	                            @RequestParam("reason") String rejectionReason,
	                            RedirectAttributes redirectAttributes,
	                            Authentication authentication) {

	    Integer reviewedByUserId = 1; // Lấy ID người dùng từ Authentication nếu muốn

	    try {
	        promotionApprovalService.rejectProductChange(approvalId, reviewedByUserId, rejectionReason);
	        redirectAttributes.addFlashAttribute("success", "Từ chối yêu cầu #" + approvalId + " thành công!");
	    } catch (RuntimeException e) {
	        redirectAttributes.addFlashAttribute("error", "Từ chối thất bại. Chi tiết: " + e.getMessage());
	    }

	    return "redirect:/admin/promotions/pending";
	}

}
