package com.alotra.repository.location;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.location.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {

	List<Address> findByUser_IdOrderByIsDefaultDescCreatedAtDesc(Integer userId);

	Optional<Address> findByUser_IdAndIsDefault(Integer userId, Boolean isDefault);

	@Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.isDefault = true")
	Optional<Address> findDefaultAddressByUser_Id(@Param("userId") Integer userId);
}
