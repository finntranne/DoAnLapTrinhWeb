package com.alotra.entity.user;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "Roles")
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "RoleID")
	private int roleid;
	
	@Column(name = "RoleName", columnDefinition = "nvarchar(20)", nullable = false, unique = true)
	private String rolename;
	
	@ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return roleid == role.roleid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleid);
    }
}
