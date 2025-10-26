package com.alotra.repository.common;

import com.alotra.entity.common.CloudinaryAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CloudinaryAssetRepository extends JpaRepository<CloudinaryAsset, Integer> {
    
    // Tìm theo publicId
    Optional<CloudinaryAsset> findByPublicId(String publicId);
    
    // Tìm theo user
    List<CloudinaryAsset> findByUploadedBy_IdOrderByUploadedAtDesc(Integer userId);
    
    // Tìm theo resource type
    List<CloudinaryAsset> findByResourceTypeOrderByUploadedAtDesc(String resourceType);
    
    // Tìm assets cũ không được sử dụng
    @Query("SELECT ca FROM CloudinaryAsset ca WHERE ca.uploadedAt < :date")
    List<CloudinaryAsset> findOldAssets(@Param("date") LocalDateTime date);
    
    // Kiểm tra publicId đã tồn tại
    boolean existsByPublicId(String publicId);
}