package com.alotra.entity.product;

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
@Table(name = "Sizes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Size {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SizeID")
    private Integer sizeID;
    
    @Column(name = "SizeName", nullable = false, unique = true, length = 20)
    private String sizeName;
    
    @Column(name = "Description", length = 100)
    private String description;
}
