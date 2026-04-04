
package com.alotra.entity.product; // Giữ package này

import jakarta.persistence.*; // Import đầy đủ JPA annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

@Entity
@Table(name = "ProductImages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "product") 
@EqualsAndHashCode(exclude = "product")
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