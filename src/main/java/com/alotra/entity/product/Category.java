
package com.alotra.entity.product; // Giữ package này

import java.util.List;

import jakarta.persistence.*; // Import đầy đủ JPA annotations
import lombok.AllArgsConstructor;
// import lombok.Builder; // Builder không cần thiết nếu dùng AllArgsConstructor
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
    
    @Column(name = "CategoryName", nullable = false, unique = true, length = 255, columnDefinition = "NVARCHAR(255)") 
    private String categoryName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)") 
    private String description;

    @Column(name = "ImageURL", length = 500) 
    private String imageURL;

    @Column(name = "Status", nullable = false) 
    private Byte status = 1; 

}