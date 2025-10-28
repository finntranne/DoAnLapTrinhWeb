package com.alotra.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.location.ShippingProvider;
import com.alotra.service.location.ShippingProviderService;


@Controller
@RequestMapping("admin/shippingProviders")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminShippingProviderController {
	
	@Autowired
	ShippingProviderService shippingProviderService;
	
	@GetMapping("")
	public String list(ModelMap model,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {
		
		Pageable pageable = PageRequest.of(page - 1, size);
		Page<ShippingProvider> providerPage = shippingProviderService.findAll(pageable);

		int totalPages = providerPage.getTotalPages();

	    model.addAttribute("shippingProviders", providerPage.getContent());
	    model.addAttribute("currentPage", page);
	    model.addAttribute("totalPages", totalPages);
	    model.addAttribute("activeMenu", "shippingProviders");

	    return "admin/shipping/list";
	}
	
	@GetMapping("/edit/{id}")
	public String editProvider(@PathVariable Integer id, ModelMap model) {
	    ShippingProvider provider = shippingProviderService.findById(id)
	    		.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp"));;
	    model.addAttribute("shippingProvider", provider);
	    model.addAttribute("activeMenu", "shippingProviders");
	    return "admin/shipping/edit";
	}

	@PostMapping("/save")
	public String updateProvider(@ModelAttribute ShippingProvider provider,  
			@RequestParam(value = "page", defaultValue = "1") int page,
			RedirectAttributes redirectAttributes) {
	    try {
	    	shippingProviderService.save(provider);
	        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật nhà vận chuyển thành công!");
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
	    }
	    return "redirect:/admin/shippingProviders?page=" + page;
	}

	 @GetMapping("/delete/{id}")
	 public String delete(@PathVariable("id") Integer id,
	                         @RequestParam(value = "page", defaultValue = "1") int page,
	                         RedirectAttributes redirectAttributes) {
	        try {
	        	ShippingProvider provider = shippingProviderService.findById(id)
	                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà vận chuyển có ID: " + id));

	            
	            provider.setStatus((byte) 0);

	            shippingProviderService.save(provider);
	            redirectAttributes.addFlashAttribute("successMessage", "Nhà vận chuyển đã được tạm khóa thành công!");
	        } catch (Exception e) {
	            redirectAttributes.addFlashAttribute("errorMessage", "Khóa nhà vận chuyển thất bại: " + e.getMessage());
	        }
	        return "redirect:/admin/shippingProviders?page=" + page;
	}
	 
	@GetMapping("/add")
    public String add(ModelMap model) {
		ShippingProvider shippingProvider = new ShippingProvider();
      
        model.addAttribute("shippingProvider", shippingProvider);
        model.addAttribute("activeMenu", "shippingProviders");

        return "admin/shipping/add";
    }
	
	@GetMapping("/search")
	public String searchProviders(
            @RequestParam(required = false) String providerName,
            @RequestParam(required = false) BigDecimal baseFee,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            ModelMap model) {


        List<ShippingProvider> shippingProviders = shippingProviderService.searchShippingProviders(providerName, baseFee, status, page);

        int totalPages = shippingProviderService.getTotalPages(providerName, baseFee, status);

  
        model.addAttribute("shippingProviders", shippingProviders);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        model.addAttribute("providerName", providerName);
        model.addAttribute("baseFee", baseFee);
        model.addAttribute("status", status);
        
        model.addAttribute("activeMenu", "shippingProviders");

        return "admin/shipping/list";
    }

	

}
