package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Topping;

@Repository
public interface ToppingRepository extends JpaRepository<Topping, Integer> {
    
    List<Topping> findByStatus(Byte status);
    
    Optional<Topping> findByToppingName(String toppingName);
    
    @Query("SELECT t FROM Topping t WHERE t.status = 1 ORDER BY t.toppingName")
    List<Topping> findAllActiveToppings();
}
