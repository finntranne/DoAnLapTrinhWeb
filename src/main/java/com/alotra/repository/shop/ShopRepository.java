package com.alotra.repository.shop;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.shop.Shop;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer>{
	
	@Query("SELECT s FROM Shop s WHERE s.shopId NOT IN (" +
	           "SELECT ds.shopId FROM DiscountPolicy d JOIN d.shops ds WHERE d.policyId = :policyId)")
	List<Shop> findShopsNotInDiscount(@Param("policyId") Integer policyId);

}
