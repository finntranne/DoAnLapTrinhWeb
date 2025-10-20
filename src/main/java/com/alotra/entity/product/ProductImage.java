package com.alotra.entity.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "ProductImages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "product")
public class ProductImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ImageID")
    private Integer imageID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;
    
    @Column(name = "ImageURL", nullable = false, length = 500)
    private String imageURL;
    
    @Column(name = "IsPrimary", nullable = false)
    private Boolean isPrimary = false;
    
    @Column(name = "DisplayOrder")
    private Integer displayOrder = 0;
}

