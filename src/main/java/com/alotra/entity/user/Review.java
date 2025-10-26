////package com.alotra.entity.user;
////
////import com.alotra.entity.order.OrderDetail;
////import com.alotra.entity.product.Product;
////
////import jakarta.persistence.*;
////import lombok.AllArgsConstructor;
////import lombok.Data;
////import lombok.NoArgsConstructor;
////
////import java.time.LocalDateTime;
////
////@Entity
////@Table(name = "Reviews")
////@Data
////@NoArgsConstructor
////@AllArgsConstructor
////public class Review {
////
////    @Id
////    @GeneratedValue(strategy = GenerationType.IDENTITY)
////    @Column(name = "ReviewID")
////    private Integer reviewId;
////
////    @ManyToOne
////    @JoinColumn(name = "CustomerID", nullable = false)
////    private Customer customer;
////
////    @ManyToOne
////    @JoinColumn(name = "ProductID", nullable = false)
////    private Product product;
////
////    @OneToOne
////    @JoinColumn(name = "OrderDetailID", nullable = false, unique = true)
////    private OrderDetail orderDetail;
////
////    @Column(name = "Rating", nullable = false)
////    private Integer rating;
////
////    @Column(name = "Comment", columnDefinition = "NVARCHAR(MAX)")
////    private String comment;
////
////    @Column(name = "MediaURLs", columnDefinition = "NVARCHAR(MAX)")
////    private String mediaUrls;
////
////    @Column(name = "ReviewDate", nullable = false)
////    private LocalDateTime reviewDate = LocalDateTime.now();
////
////    // Ensure Rating is between 1 and 5
////    @PrePersist
////    @PreUpdate
////    private void validateRating() {
////        if (rating < 1 || rating > 5) {
////            throw new IllegalArgumentException("Rating must be between 1 and 5");
////        }
////    }
////}
//
//package com.alotra.entity.user; // Đổi package nếu cần
//
//import com.alotra.entity.order.OrderDetail;
//import com.alotra.entity.product.Product;
//import com.alotra.entity.user.User; // *** SỬA: Dùng User thay vì Customer ***
//
//import jakarta.persistence.*; // Import đầy đủ JPA annotations
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.EqualsAndHashCode; // Import Exclude
//import lombok.NoArgsConstructor;
//import lombok.ToString; // Import Exclude
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "Reviews", indexes = { // Thêm index khớp DB
//    @Index(name = "IX_Reviews_ProductID", columnList = "ProductID"),
//    @Index(name = "IX_Reviews_UserID", columnList = "UserID")
//})
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@ToString(exclude = {"user", "product", "orderDetail"}) // Thêm Excludes
//@EqualsAndHashCode(exclude = {"user", "product", "orderDetail"}) // Thêm Excludes
//public class Review {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "ReviewID") // Khớp DB
//    private Integer reviewId;
//
//    // *** SỬA: Dùng User và JoinColumn UserID ***
//    @ManyToOne(fetch = FetchType.LAZY) // Thêm LAZY
//    @JoinColumn(name = "UserID", nullable = false) // Khớp DB
//    private User user;
//
//    @ManyToOne(fetch = FetchType.LAZY) // Thêm LAZY
//    @JoinColumn(name = "ProductID", nullable = false) // Khớp DB
//    private Product product;
//
//    // Quan hệ OneToOne với OrderDetail là đúng theo DB
//    @OneToOne(fetch = FetchType.LAZY) // Thêm LAZY
//    @JoinColumn(name = "OrderDetailID", nullable = false, unique = true) // Khớp DB
//    private OrderDetail orderDetail;
//
//    @Column(name = "Rating", nullable = false) // Khớp DB
//    private Integer rating; // Validation được xử lý bởi @PrePersist/@PreUpdate
//
//    @Column(name = "Comment", columnDefinition = "NVARCHAR(MAX)") // Khớp DB
//    private String comment; // Check constraint trong DB
//
//    @Column(name = "MediaURLs", columnDefinition = "NVARCHAR(MAX)") // Khớp DB
//    private String mediaUrls; // Lưu JSON array
//
//    @Column(name = "ReviewDate", nullable = false, updatable = false) // Khớp DB, thêm updatable=false
//    private LocalDateTime reviewDate;
//
//    // Thêm trường này khớp DB
//    @Column(name = "IsVerifiedPurchase", nullable = false)
//    private boolean isVerifiedPurchase = true; // Mặc định là true
//
//    @PrePersist
//    protected void onCreate() {
//        if (reviewDate == null) {
//            reviewDate = LocalDateTime.now();
//        }
//        validateRating();
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        validateRating();
//    }
//
//    // Giữ lại validation
//    private void validateRating() {
//        if (rating == null || rating < 1 || rating > 5) {
//            throw new IllegalArgumentException("Rating must be between 1 and 5");
//        }
//    }
//}