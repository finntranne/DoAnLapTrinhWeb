package com.alotra.service.shop;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alotra.entity.shop.Shop;
import com.alotra.repository.shop.ShopRepository;

@Service
public class ShopService {
	@Autowired
	ShopRepository shopRepository;
	
	public List<Shop> findAllActive(){
		return shopRepository.findByStatus((byte) 1);
	}

}
