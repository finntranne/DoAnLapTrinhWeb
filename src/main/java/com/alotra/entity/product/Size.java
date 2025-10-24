package com.alotra.entity.product;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Sizes") // Sửa lại tên bảng
@Data
public class Size {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SizeID") // Sửa lại tên cột
    private Integer sizeId;
    
    @Column(name = "SizeName", nullable = false, unique = true, columnDefinition = "NVARCHAR(50)") // Sửa lại tên cột
    private String sizeName;
}