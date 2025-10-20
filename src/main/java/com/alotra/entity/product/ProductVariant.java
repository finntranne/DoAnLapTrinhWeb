package com.alotra.entity.product;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "ProductVariants", 
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_ProductVariant_Unique", 
        columnNames = {"ProductID", "SizeID"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"product", "size"})
public class ProductVariant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VariantID")
    private Integer variantID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SizeID", nullable = false)
    private Size size;
    
    @Column(name = "Price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "Stock", nullable = false)
    private Integer stock = 0;
    
    @Column(name = "SKU", unique = true, length = 50)
    private String sku;
}