package com.alotra.entity.draft;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.Topping;
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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "ToppingDrafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "shop")
@EqualsAndHashCode(exclude = "shop")
public class ToppingDraft implements DraftEntity{
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ToppingDraftID")
    private Integer toppingDraftID;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ToppingID", nullable = true)
	private Topping topping;

@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ShopID") 
    private Shop shop;

    @Column(name = "ToppingName", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String toppingName;

    @Column(name = "Price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "Status", nullable = false)
    private Byte status = 1;

    @Column(name = "ImageURL", length = 500)
    private String imageURL;

	@Override
	public Integer getId() {
		return toppingDraftID;
	}

}
