package com.alotra.service.user.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.user.User;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.user.IUserService;

@Service
public class UserServiceImpl implements IUserService{
	
	@Autowired
    private UserRepository userRepository;


	@Override
	public Optional<User> findById(Integer id) {
		return userRepository.findById(id);
	}

	@Override
	public User save(User user) {
		return userRepository.save(user);
	}

	@Override
	public void deleteById(Integer id) {
		userRepository.deleteById(id);
		
	}

	@Override
	public boolean existsById(Integer id) {
		return userRepository.existsById(id);
	}

	@Override
	public long count() {
		return userRepository.count();
	}

	@Override
	public List<User> findByUsernameContaining(String username) {
		return userRepository.findByUsernameContaining(username);
	}

	@Override
	public Page<User> findByUsernameContaining(String username, Pageable pageable) {
		return userRepository.findByUsernameContaining(username, pageable);
	}

	@Override
	public Optional<User> findByUsername(String username) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<User> findByEmail(String email) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public User updateUser(Integer id, User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<User> findAll(Pageable pageable) {
		return userRepository.findAll(pageable);
	}

}
