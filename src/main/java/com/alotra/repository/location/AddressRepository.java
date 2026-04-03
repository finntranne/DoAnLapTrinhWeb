package com.alotra.repository.location;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.location.Address;
import com.alotra.entity.user.User;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {

    @Query("SELECT a FROM Address a ORDER BY a.isDefault DESC, a.addressID DESC")
    List<Address> findByUser_IdOrderByIsDefaultDescCreatedAtDesc(Integer userId);

    @Query("SELECT a FROM Address a WHERE a.isDefault = :isDefault ORDER BY a.addressID DESC")
    Optional<Address> findByUser_IdAndIsDefault(@Param("userId") Integer userId,
            @Param("isDefault") Boolean isDefault);

    @Query("SELECT a FROM Address a WHERE a.isDefault = true ORDER BY a.addressID DESC")
    Optional<Address> findDefaultAddressByUser_Id(@Param("userId") Integer userId);

    @Query("SELECT a FROM Address a ORDER BY a.isDefault DESC, a.addressID DESC")
    List<Address> findByUser(User user);

    @Query("SELECT a FROM Address a ORDER BY a.isDefault DESC, a.addressID DESC")
    List<Address> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT a FROM Address a WHERE a.isDefault = true ORDER BY a.addressID DESC")
    Optional<Address> findByUserIdAndIsDefaultTrue(@Param("userId") Integer userId);

    @Query("SELECT a FROM Address a ORDER BY a.isDefault DESC, a.addressID DESC")
    List<Address> findAllByOrderByIsDefaultDescAddressIDDesc();
}
