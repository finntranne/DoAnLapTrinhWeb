package com.alotra.entity.product;

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
@Table(name = "ViewedProducts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewedProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ViewedID")
    private Integer viewedID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;
    
    @Column(name = "ViewCount")
    private Integer viewCount = 1;
    
    @Column(name = "LastViewedAt", nullable = false)
    private LocalDateTime lastViewedAt = LocalDateTime.now();
}