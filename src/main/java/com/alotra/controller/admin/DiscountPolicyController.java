package com.alotra.controller.admin;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.product.Category;
import com.alotra.entity.shop.DiscountPolicy;
import com.alotra.entity.shop.Shop;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.service.shop.DiscountPolicyService;

@Controller
@RequestMapping("admin/discountPolicies")
public class DiscountPolicyController {

	@Autowired
	DiscountPolicyService discPolicyService;
	
	@Autowired
	ShopRepository shopRepository;
	
	@GetMapping("")
	public String list(ModelMap model,
	                   @RequestParam(name = "page", defaultValue = "1") int page,
	                   @RequestParam(name = "size", defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page - 1, size);
		Page<DiscountPolicy> discPolicyPage = discPolicyService.findAll(pageable);

		int totalPages = discPolicyPage.getTotalPages();

	    model.addAttribute("discountPolicies", discPolicyPage.getContent());
	    model.addAttribute("currentPage", page);
	    model.addAttribute("totalPages", totalPages);
	    model.addAttribute("activeMenu", "discountPolicies");

	    return "discountPolicies/list";
	}
	
	@GetMapping("/add")
	public String add(ModelMap model) {
		DiscountPolicy discPolicy = new DiscountPolicy();
        model.addAttribute("discountPolicy", discPolicy);
	    return "discountPolicies/add";
	}
	
	@GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Integer id, ModelMap model) {
		DiscountPolicy discPolicy = discPolicyService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy id: " + id));
       
		Set<Shop> appliedShops = discPolicy.getShops();

	    model.addAttribute("discountPolicy", discPolicy);
	    model.addAttribute("appliedShops", appliedShops);
        return "discountPolicies/detail";
    }
	
	@GetMapping("/edit/{id}")
	public String edit(@PathVariable("id") Integer id, ModelMap model) {
		DiscountPolicy discPolicy = discPolicyService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy id: " + id));  
		
		Set<Shop> appliedShops = discPolicy.getShops();
		
        model.addAttribute("discountPolicy", discPolicy);
        model.addAttribute("appliedShops", appliedShops);
        return "discountPolicies/edit";
	}
	
	@PostMapping("/save")
	public String saveDiscPolicy(@ModelAttribute("discountPolicy") DiscountPolicy discPolicy) {
	    discPolicyService.save(discPolicy);    
	    return "redirect:/admin/discountPolicies";
	}
	
	@GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id) {
        discPolicyService.deleteById(id);      
        return "redirect:/admin/discountPolicies";
    } 
	
	@GetMapping("/{id}/addShop")
	public String addShop(@PathVariable("id") Integer policyId, ModelMap model) {
	    List<Shop> shops = shopRepository.findShopsNotInDiscount(policyId);
	    model.addAttribute("shops", shops);
	    model.addAttribute("policyId", policyId);
	    return "discountPolicies/addShop";
	}

	@PostMapping("/{id}/addShop")
	public String saveShopToPolicy(
	        @PathVariable("id") Integer policyId,
	        @RequestParam("shopIds") List<Integer> shopIds) {
	    DiscountPolicy policy = discPolicyService.findById(policyId)
	            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chính sách ID: " + policyId));

	    List<Shop> selectedShops = shopRepository.findAllById(shopIds);
	    policy.getShops().addAll(selectedShops);
	    discPolicyService.save(policy);
	    return "redirect:/admin/discountPolicies";
	}

	@GetMapping("/delete/{policyId}/{shopId}")
	public String removeShopFromPolicy(@PathVariable("policyId") Integer policyId,
	                                   @PathVariable("shopId") Integer shopId) {
	    DiscountPolicy policy = discPolicyService.findById(policyId)
	            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chính sách ID: " + policyId));

	    Shop shop = shopRepository.findById(shopId)
	            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop ID: " + shopId));

	    policy.getShops().remove(shop);
	    discPolicyService.save(policy);
	    return "redirect:/admin/discountPolicies";
	}

}
