package com.alotra.controller.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.repository.user.RoleRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.shop.ShopService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("admin/shops")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminShopController {
	
	@Autowired
	ShopService shopService;
	
	@Autowired
	RoleRepository roleRepository;
	
	@Autowired
	UserRepository userRepository;

	@GetMapping("")
    public String listShops(Model model) {
        List<Shop> shops = shopService.findAll();
        model.addAttribute("shops", shops);
        model.addAttribute("activeMenu", "shops");
        return "admin/shops/list"; 
    }
	
	@GetMapping("/detail/{id}")
	public String detail(@PathVariable("id") Integer id, ModelMap model) {
		Shop shop = shopService.findById(id)
	            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop có ID: " + id));
	    model.addAttribute("shop", shop);
	    model.addAttribute("activeMenu", "shops");
	    return "admin/shops/detail";
    }
	
	 @GetMapping("/edit/{id}")
    public String editShop(@PathVariable("id") Integer id, ModelMap model) {
        Shop shop = shopService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
       
        model.addAttribute("shop", shop);
        model.addAttribute("activeMenu", "shops");
        return "admin/shops/edit";
    }
	 
	@PostMapping("/edit/{id}")
    public String updateShop(@PathVariable("id") Integer id,
                             @ModelAttribute("shop") Shop updatedShop,
                             RedirectAttributes redirectAttributes) {

		 Shop existingShop = shopService.findById(id)
	                .orElseThrow(() -> new RuntimeException("Shop không tồn tại"));

        // Cập nhật chiết khấu và trạng thái
        existingShop.setCommissionRate(updatedShop.getCommissionRate());
        existingShop.setStatus(updatedShop.getStatus());
        existingShop.setUpdatedAt(LocalDateTime.now());

        shopService.save(existingShop);
        	
        User user = existingShop.getUser();
        if (updatedShop.getStatus() == 1) {
            Role vendorRole = roleRepository.findByRoleName("VENDOR")
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò VENDOR!"));
            Set<Role> roles = new HashSet<>(user.getRoles());
            roles.clear(); 
            roles.add(vendorRole); 
            user.setRoles(roles);
            
        } else {
            Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò CUSTOMER!"));
            Set<Role> roles = new HashSet<>(user.getRoles());
            roles.clear(); 
            roles.add(customerRole); 
            user.setRoles(roles);
           
        }
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thành công!");
        return "redirect:/admin/shops";
	}
	
	 @GetMapping("/search")
	    public String searchShop(
	            @RequestParam(required = false) String shopName,
	            @RequestParam(required = false) String phoneNumber,
	            @RequestParam(required = false) String address,
	            @RequestParam(required = false) Integer status,
	            @RequestParam(defaultValue = "1") int page,
	            ModelMap model) {

	        List<Shop> shops = shopService.searchShops(shopName, phoneNumber, address, status, page);
	        int totalPages = shopService.getTotalPages(shopName, phoneNumber, address, status);

	        model.addAttribute("shops", shops);
	        model.addAttribute("currentPage", page);
	        model.addAttribute("totalPages", totalPages);

	        model.addAttribute("shopName", shopName);
	        model.addAttribute("phoneNumber", phoneNumber);
	        model.addAttribute("address", address);
	        model.addAttribute("status", status);

	        model.addAttribute("activeMenu", "shops");
	        return "admin/shops/list";
	    }
	
}

