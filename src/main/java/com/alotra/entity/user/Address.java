package com.alotra.entity.user; // Hoặc package entity của bạn

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Addresses")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "addressID")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AddressID")
    private Integer addressID;

    // Quan hệ nhiều-một: Nhiều địa chỉ thuộc về MỘT khách hàng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    @Column(name = "AddressName", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String addressName; // Ví dụ: "Nhà", "Công ty"

    @Column(name = "FullAddress", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String fullAddress; // Địa chỉ chi tiết

    @Column(name = "PhoneNumber", nullable = false, columnDefinition = "NVARCHAR(20)")
    private String phoneNumber;

    @Column(name = "RecipientName", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String recipientName; // Tên người nhận tại địa chỉ này

    @Column(name = "IsDefault", nullable = false)
    private boolean isDefault;

}