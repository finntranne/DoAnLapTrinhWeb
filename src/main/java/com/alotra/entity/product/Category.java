package com.alotra.entity.product;

import java.util.Set;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Categories") // Sửa lại tên bảng
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryID") // Sửa lại tên cột
    private Integer categoryId;

    @Column(name = "CategoryName", nullable = false, unique = true, columnDefinition = "NVARCHAR(255)") // Sửa lại tên cột
    private String categoryName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(1000)")
    private String description;
    
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Product> products;
}