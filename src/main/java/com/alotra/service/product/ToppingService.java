package com.alotra.service.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.product.Product;
import com.alotra.entity.product.Topping;
import com.alotra.entity.product.ToppingApproval;
import com.alotra.repository.product.ToppingRepository;

@Service
public class ToppingService {

    private final ToppingRepository toppingRepository;
    
    // Đặt hằng số trạng thái
    private static final Byte ACTIVE_STATUS = 1; 


    public ToppingService(ToppingRepository toppingRepository) {
        this.toppingRepository = toppingRepository;
    }

  
    public List<Topping> findAvailableGlobalToppings() {
        return toppingRepository.findByStatusAndShopIsNull(ACTIVE_STATUS);
    }

    public List<Topping> findAllByIds(List<Integer> ids) {
        return toppingRepository.findAllByToppingIDIn(ids);
    }
    
    public Page<Topping> findAllApproved(Pageable pageable) {
		return toppingRepository.findByStatus(ACTIVE_STATUS, pageable);
	}
    
   
}
