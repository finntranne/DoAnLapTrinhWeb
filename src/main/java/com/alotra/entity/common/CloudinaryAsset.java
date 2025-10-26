package com.alotra.entity.common;

import com.alotra.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CloudinaryAssets")
public class CloudinaryAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AssetID")
    private Integer assetId;

    @Column(name = "PublicID", nullable = false, unique = true, length = 255)
    private String publicId;

    @Column(name = "CloudinaryURL", nullable = false, length = 500)
    private String cloudinaryUrl;

    @Column(name = "ResourceType", nullable = false, length = 20)
    private String resourceType; // 'image', 'video', 'raw'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UploadedByUserID")
    private User uploadedBy;

    @Column(name = "UploadedAt", nullable = false, columnDefinition = "DATETIME2")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}