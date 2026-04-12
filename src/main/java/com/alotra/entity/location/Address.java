package com.alotra.entity.location;

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
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(500)")
    private String province;

    @Column(nullable = false, columnDefinition = "NVARCHAR(500)")
    private String district;

    @Column(nullable = false, columnDefinition = "NVARCHAR(500)")
    private String ward;

    @Column(name = "street_address", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String streetAddress;
    
    @Column(name = "IsDefault", nullable = false)
    private Boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;
    
}
