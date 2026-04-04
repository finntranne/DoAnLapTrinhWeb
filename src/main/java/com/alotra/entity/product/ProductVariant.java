
package com.alotra.entity.product; 

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; 
import lombok.NoArgsConstructor;
import lombok.ToString; 
@Entity
@Table(name = "ProductVariants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"product", "size"})
@EqualsAndHashCode(exclude = {"product", "size"})
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

}