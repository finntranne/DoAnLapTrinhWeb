package com.alotra.entity.shop;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "DiscountPolicies")
public class DiscountPolicy {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PolicyID")
	private int policyId;
	
	@Column(name = "PolicyName", columnDefinition = "NVARCHAR(255)")
	private String policyName;
	
	@Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
	private String description;
	
	@Column(name = "DiscountValue")
	private Double discountValue;
	
	@Column(name = "StartDate")
	private LocalDate startDate;
	
	@Column(name = "EndDate")
	private LocalDate endDate;
	
	@Column(name = "Status")
	private int status;
	
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
        name = "Shops_Discounts",
        joinColumns = @JoinColumn(name = "PolicyID"),
        inverseJoinColumns = @JoinColumn(name = "ShopID")
    )
    private Set<Shop> shops = new HashSet<>();
}
