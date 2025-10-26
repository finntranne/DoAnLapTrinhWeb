//package com.alotra.entity.product;
//
//<<<<<<< HEAD
//import java.util.Set;
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(name = "Categories") // Sửa lại tên bảng
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class Category {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "CategoryID") // Sửa lại tên cột
//    private Integer categoryId;
//
//    @Column(name = "CategoryName", nullable = false, unique = true, columnDefinition = "NVARCHAR(255)") // Sửa lại tên cột
//    private String categoryName;
//
//    @Column(name = "Description", columnDefinition = "NVARCHAR(1000)")
//    private String description;
//    
//    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Set<Product> products;
//}
//=======
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "Categories")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class Category {
//    
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "CategoryID")
//    private Integer categoryID;
//    
//    @Column(name = "CategoryName", nullable = false, unique = true, length = 255)
//    private String categoryName;
//    
//    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
//    private String description;
//    
//    @Column(name = "ImageURL", length = 500)
//    private String imageURL;
//    
//    @Column(name = "Status", nullable = false)
//    private Byte status = 1; // 0: Inactive, 1: Active
//}
//>>>>>>> lam



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

    @Column(name = "CategoryName", nullable = false, unique = true, length = 255) // Khớp DB và nhánh lam
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