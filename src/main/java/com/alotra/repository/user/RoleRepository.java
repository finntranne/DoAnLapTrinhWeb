package com.alotra.repository.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.user.Role;



@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    @Query("SELECT u FROM Role u WHERE u.roleName = :roleName")
    public Role getUserByRolename(@Param("rolename") String rolename);

    Optional<Role> findByRoleName(String roleName);
}

