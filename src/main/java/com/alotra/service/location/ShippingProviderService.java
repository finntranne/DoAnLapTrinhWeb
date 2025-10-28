package com.alotra.service.location;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.location.ShippingProvider;
import com.alotra.repository.location.ShippingProviderRepository;

@Service
public class ShippingProviderService {
	
	@Autowired
	ShippingProviderRepository shippingProviderRepository;
	

	public Optional<ShippingProvider> findById(Integer id) {
		return shippingProviderRepository.findById(id);
	}


	public Optional<ShippingProvider> getShippingProvidertById(int id) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}


	public void deleteShippingProvider(int id) {
		shippingProviderRepository.deleteById(id);
		
	}


	public Page<ShippingProvider> findAll(Pageable pageable) {
		return shippingProviderRepository.findAll(pageable);
	}

	public ShippingProvider save(ShippingProvider shippingProvider) {
		return shippingProviderRepository.save(shippingProvider);
	}


	public List<ShippingProvider> searchShippingProviders(String providerName, BigDecimal baseFee, String status, int page) {
	    if (providerName != null && providerName.isBlank()) providerName = null;
	    if (status != null && status.isBlank()) status = null;

	    Pageable pageable = PageRequest.of(page - 1, 5);
	    Page<ShippingProvider> result = shippingProviderRepository.searchShippingProviders(providerName, baseFee, status, pageable);
	    return result.getContent();
	}


	public int getTotalPages(String providerName, BigDecimal baseFee, String status) {
	    Pageable pageable = PageRequest.of(0, 5);
	    Page<ShippingProvider> result = shippingProviderRepository.searchShippingProviders(providerName, baseFee, status, pageable);
	    return result.getTotalPages();
	}
	
	public Page<ShippingProvider> searchAndSort(String providerName, String baseFee, String status, Pageable pageable) {
	    if (providerName != null && !providerName.isEmpty()) {
	        return shippingProviderRepository.findByProviderNameContaining(providerName, pageable);
	    }
	    return shippingProviderRepository.findAll(pageable);
	}


}
