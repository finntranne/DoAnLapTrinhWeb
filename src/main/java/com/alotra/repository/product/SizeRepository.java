package com.alotra.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.product.Size;

@Repository
public interface SizeRepository extends JpaRepository<Size, Integer> {
    
    Optional<Size> findBySizeName(String sizeName);
    
    List<Size> findAllByOrderBySizeNameAsc();
}