package com.alotra.service.shipping.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.shipping.ShippingProvider;
import com.alotra.repository.shipping.ShippingProviderRepository;
import com.alotra.service.shipping.ShippingProviderService;

@Service
public class ShippingProviderServiceImpl implements ShippingProviderService{

	@Autowired
	ShippingProviderRepository shippingProviderRepository;
	
	@Override
	public Optional<ShippingProvider> findById(Integer id) {
		return shippingProviderRepository.findById(id);
	}

	@Override
	public Optional<ShippingProvider> getShippingProvidertById(int id) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	

	

	@Override
	public void deleteShippingProvider(int id) {
		shippingProviderRepository.deleteById(id);
		
	}

	@Override
	public Page<ShippingProvider> findAll(Pageable pageable) {
		return shippingProviderRepository.findAll(pageable);
	}

	@Override
	public ShippingProvider save(ShippingProvider shippingProvider) {
		return shippingProviderRepository.save(shippingProvider);
	}

	@Override
	public List<ShippingProvider> searchShippingProviders(String providerName, Double baseFee, String status, int page) {
	    if (providerName != null && providerName.isBlank()) providerName = null;
	    if (status != null && status.isBlank()) status = null;

	    Pageable pageable = PageRequest.of(page - 1, 5);
	    Page<ShippingProvider> result = shippingProviderRepository.searchShippingProviders(providerName, baseFee, status, pageable);
	    return result.getContent();
	}

	@Override
	public int getTotalPages(String providerName, Double baseFee, String status) {
	    Pageable pageable = PageRequest.of(0, 5);
	    Page<ShippingProvider> result = shippingProviderRepository.searchShippingProviders(providerName, baseFee, status, pageable);
	    return result.getTotalPages();
	}


}
