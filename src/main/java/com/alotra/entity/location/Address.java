package com.alotra.entity.location;

import java.time.LocalDateTime;

import com.alotra.entity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AddressID")
    private Integer addressID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;
    
    @Column(name = "AddressName", nullable = false, length = 100)
    private String addressName;
    
    @Column(name = "FullAddress", nullable = false, length = 500)
    private String fullAddress;
    
    @Column(name = "PhoneNumber", nullable = false, length = 20)
    private String phoneNumber;
    
    @Column(name = "RecipientName", nullable = false, length = 255)
    private String recipientName;
    
    @Column(name = "IsDefault", nullable = false)
    private Boolean isDefault = false;
    
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}