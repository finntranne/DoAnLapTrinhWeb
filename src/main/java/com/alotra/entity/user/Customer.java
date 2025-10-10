package com.alotra.entity.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CustomerID")
    private Integer customerId;

    @OneToOne
    @JoinColumn(name = "UserID", nullable = false, unique = true)
    private User user;

    @Column(name = "FullName", nullable = false)
    private String fullName;

    @Column(name = "Status", nullable = false)
    private Integer status;
}