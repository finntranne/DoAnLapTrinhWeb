package com.alotra.controller.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.entity.ApprovalRequest;
import com.alotra.entity.draft.ProductDraft;
import com.alotra.entity.product.Product;
//import com.alotra.entity.product.ProductApproval;
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Topping;
import com.alotra.entity.promotion.Promotion;
import com.alotra.enums.ActionType;
import com.alotra.enums.ApprovalStatus;
import com.alotra.enums.TargetType;
import com.alotra.repository.approval_request.ApprovalRequestRepository;
import com.alotra.repository.approval_request.ProductDraftRepository;
import com.alotra.security.CustomUserDetailsService;
import com.alotra.security.MyUserDetails;
import com.alotra.service.approval_request.approval.ApprovalAdminService;
import com.alotra.service.product.ProductApprovalService;
//import com.alotra.service.product.ProductApprovalService;
import com.alotra.service.product.ProductService;
import com.alotra.service.product.ToppingService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("admin/products")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminProductController {

	@Autowired
	ProductService productService;

	@Autowired
	ProductApprovalService productApprovalService;
	
	@Autowired
	ApprovalRequestRepository approvalRequestRepository;
	
	@Autowired
	ProductDraftRepository productDraftRepository;

	@Autowired
	ToppingService toppingService;

	@GetMapping({ "", "/selling" })
	public String listSelling(ModelMap model, @RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page - 1, size);
		Page<Product> productPage = productService.findAllApproved(pageable);

		int totalPages = productPage.getTotalPages();

		model.addAttribute("products", productPage.getContent());
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", totalPages);

		model.addAttribute("activeMenu", "products");

		return "admin/products/selling";
	}

	@GetMapping("/pending")
	public String listPending(ModelMap model, @RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "10") int size) {

		int actualPage = Math.max(1, page);
		Pageable pageable = PageRequest.of(actualPage - 1, size, Sort.by("requestedAt").ascending());
//		Page<ApprovalRequest> approvalPage = productApprovalService.findByStatus(ApprovalStatus.PENDING, pageable);
		
		Page<ApprovalRequest> approvalPage = approvalRequestRepository.findByTargetType(TargetType.PRODUCT, pageable);

		int totalPages = approvalPage.getTotalPages();

		model.addAttribute("products", approvalPage.getContent());
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", totalPages);

		model.addAttribute("activeMenu", "products");

		return "admin/products/pending";
	}

	@GetMapping("/selling/detail/{id}")
	public String detail(@PathVariable("id") Integer id, ModelMap model) {
		Product product = productService.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy id: " + id));

		List<ProductVariant> variants = product.getVariants();
		Set<ProductImage> images = product.getImages();

		ProductImage primaryImage = null;
		if (images != null && !images.isEmpty()) {
		    primaryImage = images.stream()
		        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
		        .findFirst()
		        .orElse(images.iterator().next());
		}


		System.out.println("Ảnh: " + primaryImage);

		model.addAttribute("product", product);
		model.addAttribute("images", images);
		model.addAttribute("primaryImage", primaryImage);
		model.addAttribute("variants", variants);

		model.addAttribute("activeMenu", "products");
		return "admin/products/detail";
	}

	
	@GetMapping("/pending/detail/{id}")
	public String showPendingApprovalDetail(@PathVariable Integer id,
			Model model, RedirectAttributes redirectAttributes) {

		try {

			ApprovalRequest approvalRequest = approvalRequestRepository.findById(id)
					.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ApprovalRequest"));
			
			ProductDraft productDraft = productDraftRepository.findById(approvalRequest.getTargetId())
					.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ProductDraft"));
			
			model.addAttribute("approval", approvalRequest);

			model.addAttribute("product", productDraft);
			model.addAttribute("action", approvalRequest.getActionType().toString());

			model.addAttribute("variants", productDraft.getVariants());
			model.addAttribute("existingImages",  productDraft.getImages());

			model.addAttribute("availableToppings", productDraft.getAvailableToppings());

			model.addAttribute("activeMenu", "products");

			return "admin/products/approval-detail";

		} catch (Exception e) {
			log.error("Error loading product for edit", e);
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/admin/products";
		}
	}
	
	@Autowired
	private ApprovalAdminService approvalAdminService;
	
	@PostMapping("/approve/{id}")
	public String approveProduct(@PathVariable("id") Integer approvalId, 
	                            RedirectAttributes redirectAttributes) {
	    try {
	    	ApprovalRequest approvalRequest = approvalRequestRepository.findById(approvalId)
	    			.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ApprovalRequest"));
	    	
	        approvalAdminService.approve(approvalRequest);

	        redirectAttributes.addFlashAttribute("success", 
	            "Phê duyệt yêu cầu #" + approvalId + " thành công!");
	            
	    } catch (IllegalStateException e) {
	        redirectAttributes.addFlashAttribute("error", e.getMessage());
	    } catch (Exception e) {
	        log.error("Lỗi khi phê duyệt yêu cầu: {}", approvalId, e);
	        redirectAttributes.addFlashAttribute("error", "Phê duyệt thất bại: " + e.getMessage());
	    }

	    return "redirect:/admin/products/pending";
	}
	
	@PostMapping("/reject/{id}")
	public String rejectProduct(@PathVariable("id") Integer approvalId,
	                            @RequestParam("reason") String rejectionReason,
	                            RedirectAttributes redirectAttributes,
	                            Authentication authentication) {

	    try {
	    	ApprovalRequest approvalRequest = approvalRequestRepository.findById(approvalId)
	    			.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ApprovalRequest"));
	    	approvalAdminService.reject(approvalRequest, rejectionReason);
	        redirectAttributes.addFlashAttribute("success", "Từ chối yêu cầu #" + approvalId + " thành công!");
	    } catch (RuntimeException e) {
	        redirectAttributes.addFlashAttribute("error", "Từ chối thất bại. Chi tiết: " + e.getMessage());
	    }

	    return "redirect:/admin/products/pending";
	}
	
	@GetMapping("/delete/{id}")
	public String delete(@PathVariable("id") Integer id,
            @RequestParam(value = "page", defaultValue = "1") int page,
            RedirectAttributes redirectAttributes) {
	    try {
	        Product product = productService.findById(id)
	                .orElseThrow(() -> new RuntimeException("Product not found"));

	        product.setStatus((byte) 0); 
	        productService.save(product);

	        redirectAttributes.addFlashAttribute("successMessage", 
	            "Hủy sản phẩm thành công: " + product.getProductName());
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("errorMessage", 
	            "Có lỗi xảy ra khi hủy sản phẩm: " + e.getMessage());
	    }

	    return "redirect:/admin/products";
	}


}
