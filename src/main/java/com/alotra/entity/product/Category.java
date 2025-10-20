package com.alotra.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryID")
    private Integer categoryID;
    
    @Column(name = "CategoryName", nullable = false, unique = true, length = 255)
    private String categoryName;
    
    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;
    
    @Column(name = "ImageURL", length = 500)
    private String imageURL;
    
    @Column(name = "Status", nullable = false)
    private Byte status = 1; // 0: Inactive, 1: Active
}
