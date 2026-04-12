package com.alotra.entity.draft;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.shop.Shop;

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

@Entity
@Table(name = "ProductImageDrafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDraft {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productImageDraftId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductDraftID", nullable = false)
    private ProductDraft productDraft;

    @Column(nullable = false, length = 500)
    private String imageURL;

    @Column(name = "IsPrimary", nullable = false) 
    private Boolean isPrimary = false; 

    @Column(name = "DisplayOrder") 
    private Integer displayOrder = 0; 
}
