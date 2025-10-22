package com.alotra.service.shop.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.shop.DiscountPolicy;
import com.alotra.repository.shop.DiscountPolicyRepository;
import com.alotra.service.shop.DiscountPolicyService;

@Service
public class DiscountPolicyServiceImpl implements DiscountPolicyService{

	@Autowired
	DiscountPolicyRepository discPolicyRepository;
	
	@Override
	public Optional<DiscountPolicy> findById(Integer id) {
		return discPolicyRepository.findById(id);
	}

	@Override
	public DiscountPolicy save(DiscountPolicy discPolicy) {
		return discPolicyRepository.save(discPolicy);
	}

	@Override
	public void deleteById(Integer id) {
		discPolicyRepository.deleteById(id);
	}

	@Override
	public boolean existsById(Integer id) {
		return discPolicyRepository.existsById(id);
	}

	@Override
	public long count() {
		return discPolicyRepository.count();
	}

	@Override
	public Page<DiscountPolicy> findAll(Pageable pageable) {
		return discPolicyRepository.findAll(pageable);
	}

	@Override
	public List<DiscountPolicy> findByPolicyNameContaining(String policyName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<DiscountPolicy> findByPolicyNameContaining(String policyName, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<DiscountPolicy> findByPolicyName(String username) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public DiscountPolicy updateUser(Integer id, DiscountPolicy discPolicy) {
		// TODO Auto-generated method stub
		return null;
	}

}
