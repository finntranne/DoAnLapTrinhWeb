package com.alotra.controller.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.alotra.dto.category.CategoryModel;
import com.alotra.entity.product.Category;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.service.cloudinary.CloudinaryService;
import com.alotra.service.product.CategoryService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("admin/categories")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminCategoryController {

	@Autowired
	CategoryService categoryService;
	
	@Autowired
	CloudinaryService cloudinary;
	
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

	    return "admin/categories/list";
	}
	
	@GetMapping("/add")
    public String add(ModelMap model) {
        Category cate = new Category();
        model.addAttribute("category", cate);
        model.addAttribute("activeMenu", "categories");
        return "admin/categories/add";
    }

    
	@PostMapping("/save")
	public String saveCategory(@ModelAttribute("category") CategoryModel categoryModel,
			RedirectAttributes redirectAttributes) {
		try {
			if (categoryService.existsByCategoryName(categoryModel.getCategoryName())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tên danh mục đã tồn tại!");
                return "redirect:/admin/categories/add";
            }
			
			Category category = new Category();

            BeanUtils.copyProperties(categoryModel, category);
			
            if (categoryModel.getFile() != null && !categoryModel.getFile().isEmpty()) {
                try {
                    
                    String uploadedUrl = cloudinary.uploadImage(categoryModel.getFile(), "images");
                    category.setImageURL(uploadedUrl);
                } catch (Exception e) {
                    log.error("Lỗi khi upload ảnh đại diện: {}", e.getMessage(), e);
                    redirectAttributes.addFlashAttribute("errorMessage", "Tải ảnh lên thất bại. Vui lòng thử lại!");
                    return "redirect:/admin/categories/add";
                }
            }
			
			categoryService.save(category);
			redirectAttributes.addFlashAttribute("successMessage", "Danh mục mới đã được thêm thành công!");
		    return "redirect:/admin/categories";
        	
		} catch (Exception e) {
        	log.error("Lỗi khi lưu người dùng: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi lưu người dùng!");
            return "redirect:/admin/categories";
		}
	    
	}

	
	@GetMapping("/edit/{id}")
    public String editCategory(@PathVariable("id") Integer id, ModelMap model) {
        Category cate = categoryService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));     
        model.addAttribute("category", cate);
        model.addAttribute("activeMenu", "categories");
        return "admin/categories/edit";
    }
	
	@PostMapping("/edit/{id}")
	public String updateCategory(@PathVariable("id") Integer id,
	                             @ModelAttribute("category") Category category,
	                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
	                             RedirectAttributes redirectAttributes) {
	    try {
	        // 1️⃣ Tìm category hiện tại
	        Category existingCategory = categoryService.findById(id)
	                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục có ID: " + id));
	        
	        Optional<Category> duplicate = categoryService.findByCategoryName(category.getCategoryName());
	        if (duplicate.isPresent() && !duplicate.get().getCategoryID().equals(id)) {
	            redirectAttributes.addFlashAttribute("errorMessage", "Tên danh mục '" + category.getCategoryName() + "' đã tồn tại!");
	            return "redirect:/admin/categories/edit/" + id;
	        }

	        // 2️⃣ Xử lý ảnh (upload + xóa ảnh cũ nếu có)
	        if (imageFile != null && !imageFile.isEmpty()) {
	            try {
	                // Xóa ảnh cũ nếu tồn tại
	                if (existingCategory.getImageURL() != null && !existingCategory.getImageURL().isEmpty()) {
	                    String oldPublicId = cloudinary.extractPublicIdFromUrl(existingCategory.getImageURL());
	                    if (oldPublicId != null) {
	                        cloudinary.deleteImage(oldPublicId);
	                    }
	                }

	                // Upload ảnh mới lên Cloudinary
	                String uploadedUrl = cloudinary.uploadImage(imageFile, "categories", null);
	                category.setImageURL(uploadedUrl);

	            } catch (Exception e) {
	                log.error("Lỗi khi upload ảnh cho category {}: {}", existingCategory.getCategoryName(), e.getMessage());
	                redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật ảnh. Vui lòng thử lại!");
	                return "redirect:/admin/categories/edit/" + id;
	            }

	        } else {
	            // Không upload ảnh mới → giữ ảnh cũ
	            category.setImageURL(existingCategory.getImageURL());
	        }

	        // 3️⃣ Giữ lại thông tin không thay đổi
	        category.setCategoryID(existingCategory.getCategoryID());
	  

	        // 4️⃣ Lưu vào DB
	        categoryService.save(category);
	        log.info("Danh mục '{}' đã được cập nhật thành công.", category.getCategoryName());

	        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
	        return "redirect:/admin/categories";

	    } catch (IllegalArgumentException e) {
	        log.warn("Lỗi dữ liệu khi cập nhật category {}: {}", id, e.getMessage());
	        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
	        return "redirect:/admin/categories/edit/" + id;

	    } catch (Exception e) {
	        log.error("Lỗi không mong muốn khi cập nhật category {}: {}", id, e.getMessage(), e);
	        redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật danh mục!");
	        return "redirect:/admin/categories";
	    }
	}

    
	@GetMapping("/delete/{id}")
	public String delete(@PathVariable("id") Integer id,
	                     @RequestParam(value = "page", defaultValue = "1") int page,
	                     RedirectAttributes redirectAttributes) {
	    try {
	        Category category = categoryService.findById(id)
	                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

	        // ✅ Cập nhật trạng thái thay vì xóa
	        category.setStatus((byte) 0);
	        categoryService.save(category);

	        redirectAttributes.addFlashAttribute("successMessage", "Danh mục đã được tạm khóa thành công!");
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("errorMessage", "Khóa danh mục thất bại: " + e.getMessage());
	    }

	    return "redirect:/admin/categories?page=" + page;
	}

	
	@GetMapping("/search")
	public String searchCategories(ModelMap model,
             @RequestParam(required = false) String categoryName,
             @RequestParam(required = false) Integer status,
             @RequestParam(defaultValue = "1") int page) {
		
		List<Category> categories = categoryService.searchCategories(categoryName, status, page);
		
		int totalPages = categoryService.getTotalPages(categoryName, status);
		
		model.addAttribute("categories", categories);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("categoryName", categoryName);
		
		model.addAttribute("activeMenu", "categories");
		
		return "admin/categories/list"; 
	}
}
