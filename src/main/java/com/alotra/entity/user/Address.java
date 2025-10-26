////package com.alotra.entity.user; // Hoặc package entity của bạn
////
////import jakarta.persistence.*;
////import lombok.AllArgsConstructor;
////import lombok.EqualsAndHashCode;
////import lombok.Getter;
////import lombok.NoArgsConstructor;
////import lombok.Setter;
////
////@Entity
////@Table(name = "Addresses")
////@Getter 
////@Setter
////@NoArgsConstructor
////@AllArgsConstructor
////@EqualsAndHashCode(of = "addressID")
////public class Address {
////
////    @Id
////    @GeneratedValue(strategy = GenerationType.IDENTITY)
////    @Column(name = "AddressID")
////    private Integer addressID;
////
////    // Quan hệ nhiều-một: Nhiều địa chỉ thuộc về MỘT khách hàng
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(name = "CustomerID", nullable = false)
////    private Customer customer;
////
////    @Column(name = "AddressName", nullable = false, columnDefinition = "NVARCHAR(100)")
////    private String addressName; // Ví dụ: "Nhà", "Công ty"
////
////    @Column(name = "FullAddress", nullable = false, columnDefinition = "NVARCHAR(500)")
////    private String fullAddress; // Địa chỉ chi tiết
////
////    @Column(name = "PhoneNumber", nullable = false, columnDefinition = "NVARCHAR(20)")
////    private String phoneNumber;
////
////    @Column(name = "RecipientName", nullable = false, columnDefinition = "NVARCHAR(255)")
////    private String recipientName; // Tên người nhận tại địa chỉ này
////
////    @Column(name = "IsDefault", nullable = false)
////    private boolean isDefault;
////
////}
//
//
//package com.alotra.entity.user; // Giữ package này
//
//import jakarta.persistence.*; // Import đầy đủ JPA annotations
//import lombok.AllArgsConstructor;
//import lombok.EqualsAndHashCode;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import lombok.ToString; // Import Exclude
//import java.time.LocalDateTime; // Import LocalDateTime
//
//@Entity
//@Table(name = "Addresses") // Khớp DB
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@EqualsAndHashCode(of = "addressID", exclude = "user") // Exclude user
//@ToString(exclude = "user") // Exclude user
//public class Address {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "AddressID") // Khớp DB
//    private Integer addressID;
//
//    // *** SỬA: Dùng User và JoinColumn UserID ***
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "UserID", nullable = false) // Khớp DB
//    private User user;
//
//    @Column(name = "AddressName", nullable = false, length = 100) // Khớp DB, sửa length
//    private String addressName;
//
//    @Column(name = "FullAddress", nullable = false, length = 500) // Khớp DB, sửa length
//    private String fullAddress;
//
//    @Column(name = "PhoneNumber", nullable = false, length = 20) // Khớp DB, sửa length
//    private String phoneNumber;
//
//    @Column(name = "RecipientName", nullable = false, length = 255) // Khớp DB, sửa length
//    private String recipientName;
//
//    @Column(name = "IsDefault", nullable = false) // Khớp DB
//    private boolean isDefault = false; // Đặt giá trị mặc định
//
//    // Thêm trường createdAt khớp DB
//    @Column(name = "CreatedAt", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @PrePersist
//    protected void onCreate() {
//        if (createdAt == null) {
//            createdAt = LocalDateTime.now();
//        }
//    }
//}