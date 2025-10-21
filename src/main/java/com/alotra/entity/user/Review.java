package com.alotra.entity.user;

import com.alotra.entity.order.OrderDetail;
import com.alotra.entity.product.Product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReviewID")
    private Integer reviewId;

    @ManyToOne
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @OneToOne
    @JoinColumn(name = "OrderDetailID", nullable = false, unique = true)
    private OrderDetail orderDetail;

    @Column(name = "Rating", nullable = false)
    private Integer rating;

    @Column(name = "Comment", columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "MediaURLs", columnDefinition = "NVARCHAR(MAX)")
    private String mediaUrls;

    @Column(name = "ReviewDate", nullable = false)
    private LocalDateTime reviewDate = LocalDateTime.now();

    // Ensure Rating is between 1 and 5
    @PrePersist
    @PreUpdate
    private void validateRating() {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }
}