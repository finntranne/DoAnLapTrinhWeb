////package com.alotra.entity.user; // Hoặc package phù hợp của bạn
////
////import com.alotra.entity.product.Product;
////import jakarta.persistence.*;
////import lombok.Data;
////import lombok.EqualsAndHashCode;
////import lombok.ToString;
////
////@Entity
////@Table(name = "Favorites", 
////       uniqueConstraints = {
////           @UniqueConstraint(columnNames = {"CustomerID", "ProductID"})
////       })
////@Data
////public class Favorite {
////
////    @Id
////    @GeneratedValue(strategy = GenerationType.IDENTITY)
////    @Column(name = "FavoriteID")
////    private Integer favoriteId;
////
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(name = "CustomerID", nullable = false)
////    @EqualsAndHashCode.Exclude
////    @ToString.Exclude
////    private Customer customer;
////
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(name = "ProductID", nullable = false)
////    @EqualsAndHashCode.Exclude
////    @ToString.Exclude
////    private Product product;
////    
////    // Bạn có thể thêm một trường createdAt để biết họ thêm vào lúc nào
////    // @CreationTimestamp
////    // @Column(name = "CreatedAt", nullable = false, updatable = false)
////    // private Instant createdAt;
////}
//
//package com.alotra.entity.user; // Giữ package này
//
//import com.alotra.entity.product.Product;
//import jakarta.persistence.*; // Import đầy đủ JPA annotations
//import lombok.AllArgsConstructor; // Import AllArgsConstructor
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor; // Import NoArgsConstructor
//import lombok.ToString;
//import java.time.LocalDateTime; // Import LocalDateTime
//
//@Entity
//@Table(name = "Favorites", // Khớp DB
//       uniqueConstraints = { // Giữ lại unique constraint nhưng sửa tên cột
//           @UniqueConstraint(name = "UQ_Favorite_Unique", columnNames = {"UserID", "ProductID"})
//       },
//       indexes = { // Thêm indexes khớp DB
//           @Index(name = "IX_Favorites_UserID", columnList = "UserID"),
//           @Index(name = "IX_Favorites_ProductID", columnList = "ProductID")
//       }
//)
//@Data
//@NoArgsConstructor // Thêm constructor
//@AllArgsConstructor // Thêm constructor
//@ToString(exclude = {"user", "product"}) // Thêm Exclude
//@EqualsAndHashCode(exclude = {"user", "product"}) // Thêm Exclude
//public class Favorite {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "FavoriteID") // Khớp DB
//    private Integer favoriteId; // Giữ tên nhất quán
//
//    // *** SỬA: Dùng User và JoinColumn UserID ***
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "UserID", nullable = false) // Khớp DB
//    private User user;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "ProductID", nullable = false) // Khớp DB
//    private Product product;
//
//    // Thêm createdAt khớp DB
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