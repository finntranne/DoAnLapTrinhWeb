package com.alotra.entity.user; // Hoặc package phù hợp của bạn

import com.alotra.entity.product.Product;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "Favorites", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"CustomerID", "ProductID"})
       })
@Data
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FavoriteID")
    private Integer favoriteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Product product;
    
    // Bạn có thể thêm một trường createdAt để biết họ thêm vào lúc nào
    // @CreationTimestamp
    // @Column(name = "CreatedAt", nullable = false, updatable = false)
    // private Instant createdAt;
}