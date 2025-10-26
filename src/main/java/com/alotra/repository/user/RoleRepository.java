//package com.alotra.repository.user;
//
//import com.alotra.entity.user.Role;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//
//@Repository
//public interface RoleRepository extends JpaRepository<Role, Integer> {
//<<<<<<< HEAD
//
//    @Query("SELECT u FROM Role u WHERE u.roleName = :roleName")
//    public Role getUserByRolename(@Param("rolename") String rolename);
//
//    Optional<Role> findByRoleName(String roleName);
//}
//
//=======
//    Optional<Role> findByRoleName(String roleName);
//}
//>>>>>>> lam


package com.alotra.repository.user; // Giữ package này

import com.alotra.entity.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    // Giữ lại phương thức chuẩn từ nhánh lam
    Optional<Role> findByRoleName(String roleName);

    // Bỏ @Query getUserByRolename từ HEAD vì findByRoleName đã đủ
}