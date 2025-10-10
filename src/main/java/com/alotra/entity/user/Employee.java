package com.alotra.entity.user;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EmployeeID")
    private Integer employeeId;

    @OneToOne
    @JoinColumn(name = "UserID", nullable = false, unique = true)
    private User user;

    @Column(name = "FullName", nullable = false)
    private String fullName;

    @Column(name = "Status", nullable = false)
    private Integer status;
}