package com.alotra.entity.shipping;

import java.time.LocalDateTime;

import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "ShippingProviders")
public class ShippingProvider {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProviderID")
	private int providerId;
	
	@Column(name = "ProviderName", columnDefinition = "NVARCHAR(255)")
	private String providerName;
	
	@Column(name = "BaseFee")
	private Double baseFee;
	
	@Column(name = "Status")
	private int status;
}
