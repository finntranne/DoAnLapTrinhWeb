package com.alotra.repository.location;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.alotra.entity.location.ShippingProvider;

@Repository
public interface ShippingProviderRepository extends JpaRepository<ShippingProvider, Integer> {
    
    List<ShippingProvider> findByStatus(Byte status);
    
    Optional<ShippingProvider> findByProviderName(String providerName);
    
    @Query("SELECT sp FROM ShippingProvider sp WHERE sp.status = 1 ORDER BY sp.baseFee ASC")
    List<ShippingProvider> findAllActiveProviders();
}
