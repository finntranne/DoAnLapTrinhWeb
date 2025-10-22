package com.alotra.service.shop;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.alotra.entity.shop.DiscountPolicy;

public interface DiscountPolicyService {
	
	Optional<DiscountPolicy> findById(Integer id);
    DiscountPolicy save(DiscountPolicy discPolicy);
    void deleteById(Integer id);
    boolean existsById(Integer id);
    long count();
    Page<DiscountPolicy> findAll(Pageable pageable);

    // ===== SEARCH =====
    List<DiscountPolicy> findByPolicyNameContaining(String policyName);
    Page<DiscountPolicy> findByPolicyNameContaining(String policyName, Pageable pageable);

    // ===== EXTRA =====
    Optional<DiscountPolicy> findByPolicyName(String username);
    DiscountPolicy updateUser(Integer id, DiscountPolicy discPolicy);

}
