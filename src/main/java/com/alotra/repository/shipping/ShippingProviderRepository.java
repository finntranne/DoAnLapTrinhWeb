package com.alotra.repository.shipping;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.alotra.entity.shipping.ShippingProvider;


public interface ShippingProviderRepository extends JpaRepository<ShippingProvider, Integer>{

	@Query("""
		    SELECT p FROM ShippingProvider p
		    WHERE (:providerName IS NULL OR LOWER(p.providerName) LIKE LOWER(CONCAT('%', :providerName, '%')))
		      AND (:baseFee IS NULL OR p.baseFee = :baseFee)
		      AND (:status IS NULL OR CAST(p.status AS string) = :status)
		""")
		Page<ShippingProvider> searchShippingProviders(
		    @Param("providerName") String providerName,
		    @Param("baseFee") Double baseFee,
		    @Param("status") String status,
		    Pageable pageable
		);

}
