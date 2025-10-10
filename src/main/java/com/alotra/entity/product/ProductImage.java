package com.alotra.entity.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp; // <-- THÊM IMPORT NÀY
import java.time.Instant;

@Entity
@Table(name = "ProductImages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ImageID")
    private Integer imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Product product;

    @Column(name = "ImageURL", nullable = false)
    private String imageUrl;

    @Column(name = "IsPrimary", nullable = false)
    private Boolean isPrimary;
    
    // Bỏ các trường không cần thiết cho việc hiển thị ban đầu để đơn giản
    // @Column(name = "DisplayOrder", nullable = false)
    // private Integer displayOrder;
    // @Column(name = "Status", nullable = false)
    // private Byte status;

    @CreationTimestamp // <-- THÊM DÒNG NÀY
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private Instant createdAt;
}