package com.alotra.entity.user;


import java.io.Serializable;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "UserRoles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "UserID")
    private User user;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "RoleID")
    private Role role;
}

@Embeddable
class UserRoleId implements Serializable {
    private Integer userId;
    private Integer roleId;

    // Constructors, getters, setters, equals, hashCode
}