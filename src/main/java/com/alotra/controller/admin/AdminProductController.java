package com.alotra.controller.admin;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductApproval;
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Topping;
import com.alotra.security.CustomUserDetailsService;
import com.alotra.service.product.ProductApprovalService;
import com.alotra.service.product.ProductService;
import com.alotra.service.product.ToppingService;
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
		Pageable pageable = PageRequest.of(actualPage - 1, size, Sort.by("requestedAt").descending());
		Page<ProductApproval> approvalPage = productApprovalService.findByStatus("PENDING", pageable);
		System.out.print(approvalPage);

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
	public String showPendingApprovalDetail(@PathVariable("id") Integer approvalId, ModelMap model) {

		Optional<ProductApproval> approvalOpt = productApprovalService.findById(approvalId);

		if (approvalOpt.isEmpty()) {
			model.addAttribute("errorMessage", "Không tìm thấy yêu cầu phê duyệt có ID: " + approvalId);
			return "error/404";
		}

		List<Topping> availableGlobalToppings = toppingService.findAvailableGlobalToppings();

		ProductApproval approval = approvalOpt.get();

		model.addAttribute("approval", approval);
		model.addAttribute("availableGlobalToppings", availableGlobalToppings);

		model.addAttribute("activeMenu", "products");
		return "admin/products/approval-detail";
	}

	@PostMapping("/approve/{id}")
	public String approveProduct(@PathVariable("id") Integer approvalId, RedirectAttributes redirectAttributes,
			Authentication authentication) {


		Integer reviewedByUserId = 1;
		

		try {
			productApprovalService.approveProductChange(approvalId, reviewedByUserId);
			redirectAttributes.addFlashAttribute("success", "Phê duyệt yêu cầu #" + approvalId + " thành công!");
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("error", "Phê duyệt thất bại. Chi tiết: " + e.getMessage());
		}

		return "redirect:/admin/products/pending";

	}
	
	@PostMapping("/reject/{id}")
	public String rejectProduct(@PathVariable("id") Integer approvalId,
	                            @RequestParam("reason") String rejectionReason,
	                            RedirectAttributes redirectAttributes,
	                            Authentication authentication) {

	    Integer reviewedByUserId = 1; // Lấy ID người dùng từ Authentication nếu muốn

	    try {
	        productApprovalService.rejectProductChange(approvalId, reviewedByUserId, rejectionReason);
	        redirectAttributes.addFlashAttribute("success", "Từ chối yêu cầu #" + approvalId + " thành công!");
	    } catch (RuntimeException e) {
	        redirectAttributes.addFlashAttribute("error", "Từ chối thất bại. Chi tiết: " + e.getMessage());
	    }

	    return "redirect:/admin/products/pending";
	}


}
