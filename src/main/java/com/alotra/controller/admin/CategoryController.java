package com.alotra.controller.admin;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.BeanUtils;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.product.Category;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.service.product.CategoryService;

@Controller
@RequestMapping("admin/categories")
public class CategoryController {
	
	@Autowired
	CategoryService categoryService;
	
	@GetMapping("")
	public String list(ModelMap model,
	                   @RequestParam(name = "page", defaultValue = "1") int page,
	                   @RequestParam(name = "size", defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page - 1, size);
		Page<Category> categoryPage = categoryService.findAll(pageable);

		int totalPages = categoryPage.getTotalPages();

	    model.addAttribute("categories", categoryPage.getContent());
	    model.addAttribute("currentPage", page);
	    model.addAttribute("totalPages", totalPages);
	    model.addAttribute("activeMenu", "categories");

	    return "categories/list";
	}
	
	@GetMapping("/add")
    public String add(ModelMap model) {
        Category cate = new Category();
        model.addAttribute("category", cate);

        return "categories/add";
    }

    
	@PostMapping("/save")
	public String saveCategory(@ModelAttribute("category") Category category) {
	    categoryService.save(category);
	    return "redirect:/admin/categories";
	}

	
	@GetMapping("/edit/{id}")
    public String editUser(@PathVariable("id") Integer id, ModelMap model) {
        Category cate = categoryService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));     
        model.addAttribute("category", cate);
        return "categories/edit";
    }
    
	@GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id,
                         @RequestParam(value = "page", defaultValue = "1") int page,
                         RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Danh mục đã được xóa thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa danh mục thất bại: " + e.getMessage());
        }
        return "redirect:/admin/categories?page=" + page;
    } 
	
	@GetMapping("/search")
	public String searchCategories(ModelMap model,
             @RequestParam(required = false) String keyword,
             @RequestParam(defaultValue = "1") int page) {
		Page<Category> pageData = categoryService.searchCategories(keyword, page);
		
		model.addAttribute("categories", pageData.getContent());
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", pageData.getTotalPages());
		model.addAttribute("keyword", keyword);
		
		return "categories/search"; // file search.html
	}

}
