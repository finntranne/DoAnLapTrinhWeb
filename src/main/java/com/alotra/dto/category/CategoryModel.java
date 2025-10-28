package com.alotra.dto.category;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.alotra.entity.product.Product;

import lombok.Data;

@Data
public class CategoryModel {
	
	private Integer categoryID; 

   
    private String categoryName;

    
    private String description;

   
    private String imageURL;

    
    private Byte status = 1; // 0: Inactive, 1: Active - Giữ giá trị mặc định
    
    private List<Product> products;
    
    private MultipartFile file;


}
