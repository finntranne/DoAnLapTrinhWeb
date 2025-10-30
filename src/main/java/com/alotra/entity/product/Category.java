
package com.alotra.entity.product; // Giữ package này

import java.util.List;

import jakarta.persistence.*; // Import đầy đủ JPA annotations
import lombok.AllArgsConstructor;
// import lombok.Builder; // Builder không cần thiết nếu dùng AllArgsConstructor
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Categories") // Khớp DB
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryID") // Khớp DB và nhánh lam
    private Integer categoryID; // Giữ tên theo nhánh lam

    @Column(name = "CategoryName", nullable = false, unique = true, length = 255, columnDefinition = "NVARCHAR(255)") // Khớp DB và nhánh lam
    private String categoryName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)") // Khớp DB và nhánh lam
    private String description;

    @Column(name = "ImageURL", length = 500) // Giữ lại từ nhánh lam, khớp DB
    private String imageURL;

    @Column(name = "Status", nullable = false) // Giữ lại từ nhánh lam, khớp DB
    private Byte status = 1; // 0: Inactive, 1: Active - Giữ giá trị mặc định
    
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products;


    // Bỏ @OneToMany Set<Product> products từ HEAD vì không cần thiết và không khớp nhánh lam/DB
}