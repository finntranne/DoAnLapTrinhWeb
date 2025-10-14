package com.alotra.service.user.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.repository.user.RoleRepository;
import com.alotra.service.user.IRoleService;

@Service
public class RoleServiceImpl implements IRoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

	@Override
	public Set<Role> getUserRoles(Integer userId) {
		// TODO Auto-generated method stub
		return null;
	}
    
  


}
