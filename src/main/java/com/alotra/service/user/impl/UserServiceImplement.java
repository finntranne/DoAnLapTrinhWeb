package com.alotra.service.user.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.user.User;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.user.IUserService;

@Service
public class UserServiceImplement implements IUserService{
	
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
	public List<User> searchUsers(String username, String email, String phoneNumber, Integer roleid, Integer status, LocalDate startDate, LocalDate endDate, int page) {
		if (username != null && username.isBlank()) username = null;
	    if (email != null && email.isBlank()) email = null;
	    if (phoneNumber != null && phoneNumber.isBlank()) email = null;

	    Pageable pageable = PageRequest.of(page - 1, 10);
	    
	    LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
	    LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

	    Page<User> result = userRepository.searchUsers(username, email, phoneNumber, roleid, status, start, end, pageable);
	    
	    return result.getContent();
	}

	@Override
	public int getTotalPages(String username, String email, String phoneNumber, Integer roleid, Integer status, LocalDate startDate, LocalDate endDate) {
		 Pageable pageable = PageRequest.of(0, 10);
		 LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
		 LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;
	    Page<User> result = userRepository.searchUsers(username, email, phoneNumber, roleid, status, start, end, pageable);
	    return result.getTotalPages();
	}

	@Override
	public Page<User> findAll(Pageable pageable) {
		return userRepository.findAll(pageable);
	}

	@Override
	public Page<User> findAllWithoutAdmin(Pageable pageable) {
		return userRepository.findAllWithoutAdmin(pageable);
	}

	@Override
	public boolean existsByUsername(String username) {
		return userRepository.existsByUsername(username);
	}

	@Override
	public boolean existsByPhoneNumber(String phoneNumber) {
		return userRepository.existsByPhoneNumber(phoneNumber);
	}

	@Override
	public boolean existsByEmail(String email) {
		return userRepository.existsByEmail(email);
	}

}
