package com.alotra.entity.draft;

import java.math.BigDecimal;
import java.util.List;

import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Size;
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
@Table(name = "ProductVariantDrafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productVariantDraftId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductDraftID", nullable = false)
    private ProductDraft productDraft;

    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "SizeID", nullable = false) 
    private Size size;

    @Column(name = "Price", nullable = false, precision = 10, scale = 2) 
    private BigDecimal price;
}