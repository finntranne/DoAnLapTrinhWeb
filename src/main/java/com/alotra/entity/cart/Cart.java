package com.alotra.entity.cart;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import com.alotra.entity.user.Customer;

@Entity
@Table(name = "Carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CartID")
    private Integer cartId;

    @ManyToOne
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    @Column(name = "Status", nullable = false)
    private String status; // 'ACTIVE', 'CHECKED_OUT', 'CANCELLED'

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    private Instant updatedAt;
}
