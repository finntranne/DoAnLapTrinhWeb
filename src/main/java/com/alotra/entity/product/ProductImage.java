//package com.alotra.entity.product;
//
//<<<<<<< HEAD
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp; // <-- THÊM IMPORT NÀY
//import java.time.Instant;
//
//@Entity
//@Table(name = "ProductImages")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class ProductImage {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "ImageID")
//    private Integer imageId;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "ProductID", nullable = false)
//    @EqualsAndHashCode.Exclude
//    @ToString.Exclude
//    private Product product;
//
//    @Column(name = "ImageURL", nullable = false, columnDefinition = "NVARCHAR(2083)")
//    private String imageUrl;
//
//    @Column(name = "IsPrimary", nullable = false)
//    private Boolean isPrimary;
//
//    @CreationTimestamp // <-- THÊM DÒNG NÀY
//    @Column(name = "CreatedAt", nullable = false, updatable = false)
//    private Instant createdAt;
//}
//=======
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.ToString;
//
//@Entity
//@Table(name = "ProductImages")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@ToString(exclude = "product")
//public class ProductImage {
//    
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "ImageID")
//    private Integer imageID;
//    
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "ProductID", nullable = false)
//    private Product product;
//    
//    @Column(name = "ImageURL", nullable = false, length = 500)
//    private String imageURL;
//    
//    @Column(name = "IsPrimary", nullable = false)
//    private Boolean isPrimary = false;
//    
//    @Column(name = "DisplayOrder")
//    private Integer displayOrder = 0;
//}
//
//>>>>>>> lam


package com.alotra.entity.product; // Giữ package này

import jakarta.persistence.*; // Import đầy đủ JPA annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

@Entity
@Table(name = "ProductImages") // Khớp DB
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "product") // Giữ Exclude từ nhánh lam
@EqualsAndHashCode(exclude = "product") // Thêm EqualsAndHashCode Exclude
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ImageID") // Khớp DB và nhánh lam
    private Integer imageID; // Giữ tên theo nhánh lam

    @ManyToOne(fetch = FetchType.LAZY) // Giữ LAZY fetch
    @JoinColumn(name = "ProductID", nullable = false) // Khớp DB
    private Product product;

    @Column(name = "ImageURL", nullable = false, length = 500) // Giữ lại length 500 từ nhánh lam, khớp DB
    private String imageURL;

    @Column(name = "IsPrimary", nullable = false) // Khớp DB
    private Boolean isPrimary = false; // Giữ giá trị mặc định từ nhánh lam

    @Column(name = "DisplayOrder") // Giữ lại từ nhánh lam, khớp DB
    private Integer displayOrder = 0; // Giữ giá trị mặc định từ nhánh lam

    // Bỏ trường createdAt từ HEAD vì không có trong DB schema
}