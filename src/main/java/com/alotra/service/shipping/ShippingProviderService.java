package com.alotra.service.shipping;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.alotra.entity.shipping.ShippingProvider;



public interface ShippingProviderService {
	
	Optional<ShippingProvider> findById(Integer id);
	

    Optional<ShippingProvider> getShippingProvidertById(int id);
  
    ShippingProvider save(ShippingProvider shippingProvider);
    void deleteShippingProvider(int id);

    
    
    Page<ShippingProvider> findAll(Pageable pageable);
    
    public List<ShippingProvider> searchShippingProviders(String providerName, Double baseFee, String status, int page);
	public int getTotalPages(String providerName, Double baseFee, String status);



    
}
