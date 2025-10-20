package com.alotra.entity.product;

import java.time.LocalDateTime;

import com.alotra.entity.order.OrderDetail;
import com.alotra.entity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReviewID")
    private Integer reviewID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
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
    private String mediaURLs;
    
    @Column(name = "ReviewDate", nullable = false)
    private LocalDateTime reviewDate = LocalDateTime.now();
    
    @Column(name = "IsVerifiedPurchase", nullable = false)
    private Boolean isVerifiedPurchase = true;
}
