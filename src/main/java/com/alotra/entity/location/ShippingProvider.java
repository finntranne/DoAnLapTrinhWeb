package com.alotra.entity.location;

import java.math.BigDecimal;

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
@Table(name = "ShippingProviders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingProvider {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ProviderID")
	private Integer providerID;

	@Column(name = "ProviderName", nullable = false, unique = true, length = 255)
	private String providerName;

	@Column(name = "BaseFee", nullable = false, precision = 10, scale = 2)
	private BigDecimal baseFee = BigDecimal.ZERO;

	@Column(name = "Description", length = 500)
	private String description;

	@Column(name = "Status", nullable = false)
	private Byte status = 1; // 0: Inactive, 1: Active
}