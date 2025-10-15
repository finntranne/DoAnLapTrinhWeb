package com.alotra.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alotra.entity.user.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

}
