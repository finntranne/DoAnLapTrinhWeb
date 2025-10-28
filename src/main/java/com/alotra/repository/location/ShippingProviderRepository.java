package com.alotra.repository.location;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.location.ShippingProvider;

@Repository
public interface ShippingProviderRepository extends JpaRepository<ShippingProvider, Integer> {
    
    List<ShippingProvider> findByStatus(Byte status);
    
    Optional<ShippingProvider> findByProviderName(String providerName);
    
    @Query("SELECT sp FROM ShippingProvider sp WHERE sp.status = 1 ORDER BY sp.baseFee ASC")
    List<ShippingProvider> findAllActiveProviders();
    
    @Query("""
		    SELECT p FROM ShippingProvider p
		    WHERE (:providerName IS NULL OR LOWER(p.providerName) LIKE LOWER(CONCAT('%', :providerName, '%')))
		      AND (:baseFee IS NULL OR p.baseFee = :baseFee)
		      AND (:status IS NULL OR CAST(p.status AS string) = :status)
		""")
	Page<ShippingProvider> searchShippingProviders(
	    @Param("providerName") String providerName,
	    @Param("baseFee") BigDecimal baseFee,
	    @Param("status") String status,
	    Pageable pageable
	);

	Page<ShippingProvider> findByProviderNameContaining(String providerName, Pageable pageable);
}
