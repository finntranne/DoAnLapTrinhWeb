package com.alotra.entity.user;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @Column(name = "RoleID")
    private Integer roleId;

    @Column(name = "RoleName", nullable = false, unique = true)
    private String roleName;
}