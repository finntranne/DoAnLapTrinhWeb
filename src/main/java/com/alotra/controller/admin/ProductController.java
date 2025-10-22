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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductImage;
import com.alotra.service.product.ProductService;

@Controller
@RequestMapping("admin/products")
public class ProductController {

	@Autowired
	ProductService productService;
	
	@GetMapping("")
	public String list(ModelMap model,
	                   @RequestParam(name = "page", defaultValue = "1") int page,
	                   @RequestParam(name = "size", defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page - 1, size);
		Page<Product> productPage = productService.findAll(pageable);

		int totalPages = productPage.getTotalPages();

	    model.addAttribute("products", productPage.getContent());
	    model.addAttribute("currentPage", page);
	    model.addAttribute("totalPages", totalPages);
	    model.addAttribute("activeMenu", "products");

	    return "products/list";
	}
	
	@GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Integer id, ModelMap model) {
		Product product = productService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy id: " + id));
       
		List<ProductImage> images = product.getImages();

	    ProductImage primaryImage = null;
	    if (images != null && !images.isEmpty()) {
	        primaryImage = images.stream()
	                .filter(ProductImage::isPrimary)
	                .findFirst()
	                .orElse(images.get(0)); 
	    }

	    if (images != null && !images.isEmpty()) {
	        System.out.println("=== Danh sách ảnh của sản phẩm ID: " + id + " ===");
	        for (ProductImage img : images) {
	            System.out.println("Ảnh: " + img.getImageUrl());
	        }
	    }

	  ///  System.out.print(primaryImage);
	    model.addAttribute("product", product);
	    model.addAttribute("images", images);
	    model.addAttribute("primaryImage", primaryImage);
	    
        return "products/detail";
    }
	
	@GetMapping("/search")
	public String searchProducts(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            ModelMap model) {


        List<Product> products = productService.searchProducts(productName, categoryName, shopName, status, page);

        int totalPages = productService.getTotalPages(productName, categoryName, shopName, status);

  
        model.addAttribute("products", products);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        model.addAttribute("productName", productName);
        model.addAttribute("categoryName", categoryName);
        model.addAttribute("shopName", shopName);
        model.addAttribute("status", status);

        return "products/list";
    }
}
