//package com.alotra.entity.product;
//
//<<<<<<< HEAD
//import jakarta.persistence.*;
//import lombok.Data;
//
//@Entity
//@Table(name = "Sizes") // Sửa lại tên bảng
//@Data
//public class Size {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "SizeID") // Sửa lại tên cột
//    private Integer sizeId;
//    
//    @Column(name = "SizeName", nullable = false, unique = true, columnDefinition = "NVARCHAR(50)") // Sửa lại tên cột
//    private String sizeName;
//}
//=======
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "Sizes")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class Size {
//    
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "SizeID")
//    private Integer sizeID;
//    
//    @Column(name = "SizeName", nullable = false, unique = true, length = 20)
//    private String sizeName;
//    
//    @Column(name = "Description", length = 100)
//    private String description;
//}
//>>>>>>> lam


package com.alotra.entity.product; // Giữ package này

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Sizes") // Khớp DB
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SizeID") // Khớp DB và nhánh lam
    private Integer sizeID; // Giữ tên theo nhánh lam

    @Column(name = "SizeName", nullable = false, unique = true, length = 20) // Khớp DB và nhánh lam
    private String sizeName;

    @Column(name = "Description", length = 100) // Giữ lại từ nhánh lam, khớp DB
    private String description;
}