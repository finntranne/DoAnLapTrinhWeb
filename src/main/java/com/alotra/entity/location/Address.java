package com.alotra.entity.location;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.alotra.entity.user.Customer;

@Entity
@Table(name = "Addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AddressID")
    private Integer addressId;

    @ManyToOne
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    @Column(name = "AddressName", nullable = false)
    private String addressName;

    @Column(name = "FullAddress", nullable = false)
    private String fullAddress;

    @Column(name = "PhoneNumber", nullable = false)
    private String phoneNumber;

    @Column(name = "RecipientName", nullable = false)
    private String recipientName;

    @Column(name = "IsDefault", nullable = false)
    private Boolean isDefault;

    @Column(name = "Status", nullable = false)
    private Integer status;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;
}
