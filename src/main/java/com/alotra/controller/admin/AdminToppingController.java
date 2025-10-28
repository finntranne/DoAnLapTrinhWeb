package com.alotra.controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import com.alotra.entity.product.Topping;
import com.alotra.entity.product.ToppingApproval;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.service.product.ToppingApprovalService;
import com.alotra.service.product.ToppingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("admin/toppings")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminToppingController {
	
	@Autowired
	ToppingService toppingService;
	
	@Autowired
	ToppingApprovalService toppingApprovalService;

	@GetMapping({ "", "/selling" })
	public String listSelling(ModelMap model, @RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page - 1, size);
		Page<Topping> toppingPage = toppingService.findAllApproved(pageable);

		int totalPages = toppingPage.getTotalPages();

		model.addAttribute("toppings", toppingPage.getContent());
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", totalPages);

		model.addAttribute("activeMenu", "toppings");

		return "admin/toppings/selling";
	}
	
	@GetMapping("/pending")
	public String listPending(ModelMap model, @RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "10") int size) {
		int actualPage = Math.max(1, page);
		Pageable pageable = PageRequest.of(actualPage - 1, size, Sort.by("requestedAt").descending());
		Page<ToppingApproval> approvalPage = toppingApprovalService.findByStatus("PENDING", pageable);
		System.out.print(approvalPage.getContent());

		int totalPages = approvalPage.getTotalPages();

		model.addAttribute("toppings", approvalPage.getContent());
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", totalPages);

		model.addAttribute("activeMenu", "toppings");

		return "admin/toppings/pending";
	}
	
	@GetMapping("/pending/detail/{id}")
	public String showPendingApprovalDetail(@PathVariable("id") Integer approvalId, ModelMap model) {

		Optional<ToppingApproval> approvalOpt = toppingApprovalService.findById(approvalId);

		if (approvalOpt.isEmpty()) {
			model.addAttribute("errorMessage", "Không tìm thấy yêu cầu phê duyệt có ID: " + approvalId);
			return "error/404";
		}

		ToppingApproval approval = approvalOpt.get();
		
		Map<String, Object> changeMap = new HashMap<>();
		if ("UPDATE".equals(approval.getActionType()) && approval.getChangeDetails() != null) {
		    ObjectMapper mapper = new ObjectMapper();
		    try {
		        
		        changeMap = mapper.readValue(
		            approval.getChangeDetails(),
		            new TypeReference<Map<String, Object>>() {}
		        );
		    } catch (JsonProcessingException e) {
		        e.printStackTrace(); 
		    }
		}

		model.addAttribute("approval", approval);

		model.addAttribute("changeMap", changeMap);

		model.addAttribute("activeMenu", "toppings");
		return "admin/toppings/approval-detail";
	}
	
	@PostMapping("/approve/{id}")
	public String approveProduct(@PathVariable("id") Integer approvalId, RedirectAttributes redirectAttributes,
			Authentication authentication) {


		Integer reviewedByUserId = 1;
		

		try {
			toppingApprovalService.approveToppingChange(approvalId, reviewedByUserId);
			redirectAttributes.addFlashAttribute("success", "Phê duyệt yêu cầu #" + approvalId + " thành công!");
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("error", "Phê duyệt thất bại. Chi tiết: " + e.getMessage());
		}

		return "redirect:/admin/toppings/pending";

	}

}
