package com.alotra.service.shop;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.product.Category;
import com.alotra.entity.shop.Shop;
import com.alotra.repository.shop.ShopRepository;

@Service
public class ShopService {
	@Autowired
	ShopRepository shopRepository;
	
	public List<Shop> findAllActive(){
		return shopRepository.findByStatus((byte) 1);
	}
	
	public List<Shop> findAll(){
		return shopRepository.findAll();
	}
	
	public Optional<Shop> findById(Integer id) {
		return shopRepository.findById(id);
	}
	
	public Shop save(Shop shop) {
		return shopRepository.save(shop);
	}
	
	public List<Shop> searchShops(String shopName, String phoneNumber, String address, Integer status, int page){
		if (shopName != null && shopName.isBlank()) shopName = null;
		if (phoneNumber != null && phoneNumber.isBlank()) phoneNumber = null;
		if (address != null && address.isBlank()) address = null;
		
	    Pageable pageable = PageRequest.of(page - 1, 10);

	    Page<Shop> result = shopRepository.searchShops(shopName, phoneNumber, address, status, pageable);
	    
	    return result.getContent();
	}
	
	public int getTotalPages(String shopName, String phoneNumber, String address, Integer status) {
		 Pageable pageable = PageRequest.of(0, 10);
		
	    Page<Shop> result = shopRepository.searchShops(shopName, phoneNumber, address, status, pageable);
	    return result.getTotalPages();
	}

}
