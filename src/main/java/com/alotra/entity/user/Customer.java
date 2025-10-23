package com.alotra.entity.user;

import java.util.Set;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Customers")
@Getter // <-- Thay thế @Data
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "customerId")
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
    
    @Column(name = "Email", nullable = true, unique = true)
    private String email;

    @Column(name = "Status", nullable = false)
    private Integer status;
    
 // Quan hệ một-nhiều: Một khách hàng có NHIỀU địa chỉ
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Address> addresses;
}