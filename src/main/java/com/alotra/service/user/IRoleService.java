package com.alotra.service.user;

import java.util.List;
import java.util.Set;

import com.alotra.entity.user.Role;

public interface IRoleService {

	List<Role> findAll();
	
	Set<Role> getUserRoles(Integer userId);
}
