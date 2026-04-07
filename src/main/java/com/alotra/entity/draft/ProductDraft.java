package com.alotra.entity.draft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.Topping;
import com.alotra.entity.shop.Shop;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "ProductDrafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"shop", "category", "variants", "images", "availableToppings"})
@EqualsAndHashCode(exclude = {"shop", "category", "variants", "images", "availableToppings"})
public class ProductDraft {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductDraftID")
    private Integer productDraftID;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ProductID", nullable = true)
	private Product product;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ShopID", nullable = false)
	private Shop shop;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CategoryID")
	private Category category;
	
	@Column(name = "ProductName", length = 255, columnDefinition = "NVARCHAR(255)")
	private String productName;
	
	@Column(name = "BasePrice", precision = 18, scale = 2)
	private BigDecimal basePrice;
	
	@Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
	private String description;
	
	@Column(name = "Status", nullable = false)
	private Byte status;
	
	@OneToMany(mappedBy = "productDraft", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<ProductVariantDraft> variants = new ArrayList<>();
	
	@OneToMany(mappedBy = "productDraft", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private Set<ProductImageDraft> images = new HashSet<>();
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
	    name = "ProductDraftAvailableToppings",
	    joinColumns = @JoinColumn(name = "ProductDraftID"),
	    inverseJoinColumns = @JoinColumn(name = "ToppingID")
	)
	private Set<Topping> availableToppings = new HashSet<>();
}
