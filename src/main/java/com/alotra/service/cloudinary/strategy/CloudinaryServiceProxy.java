package com.alotra.service.cloudinary.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy Pattern Implementation for CloudinaryService
 * Adds cross-cutting concerns:
 * - Caching of upload results
 * - Request validation and logging
 * - Error handling
 * - Upload history tracking
 */
@Component
@Slf4j
public class CloudinaryServiceProxy implements UploadStrategy {

    private final UploadStrategy uploadStrategy;
    
    // Cache for recent uploads (pulic_id -> upload result)
    private final Map<String, Map<String, String>> uploadCache = new ConcurrentHashMap<>();
    
    // Upload history tracking
    private final Map<String, UploadHistoryRecord> uploadHistory = new ConcurrentHashMap<>();
    
    // Upload statistics
    private long totalUploads = 0;
    private long totalImageUploads = 0;
    private long totalVideoUploads = 0;
    private long totalUploadErrors = 0;

    @Autowired
    public CloudinaryServiceProxy(CloudinaryUploadStrategy cloudinaryUploadStrategy) {
        this.uploadStrategy = cloudinaryUploadStrategy;
    }

    @Override
    public Map<String, String> uploadImage(MultipartFile file, String folder, Integer userId) throws IOException {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("[PROXY] Starting image upload - File: {}, Folder: {}, Size: {} bytes", 
                    file.getOriginalFilename(), folder, file.getSize());

            // Validate file before proceeding
            FileValidationUtil.validateImageFile(file);

            // Check cache (optional)
            String cacheKey = generateCacheKey(file, folder);
            if (uploadCache.containsKey(cacheKey)) {
                log.debug("[PROXY] Cache hit for image upload: {}", cacheKey);
                return uploadCache.get(cacheKey);
            }

            // Delegate to actual upload strategy
            Map<String, String> result = uploadStrategy.uploadImage(file, folder, userId);

            // Cache the result
            uploadCache.put(cacheKey, result);

            // Record in history
            recordUploadHistory(file.getOriginalFilename(), folder, "image", result, userId, startTime);

            totalUploads++;
            totalImageUploads++;

            long duration = System.currentTimeMillis() - startTime;
            log.info("[PROXY] Image upload completed successfully in {} ms - URL: {}", 
                    duration, result.get("secure_url"));

            return result;

        } catch (IllegalArgumentException e) {
            totalUploadErrors++;
            log.error("[PROXY] Validation error during image upload: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            totalUploadErrors++;
            log.error("[PROXY] IOException during image upload: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            totalUploadErrors++;
            log.error("[PROXY] Unexpected error during image upload: {}", e.getMessage(), e);
            throw new RuntimeException("[PROXY] Unexpected error uploading image: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> uploadVideo(MultipartFile file, String folder, Integer userId) throws IOException {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("[PROXY] Starting video upload - File: {}, Folder: {}, Size: {} bytes", 
                    file.getOriginalFilename(), folder, file.getSize());

            // Validate file before proceeding
            FileValidationUtil.validateVideoFile(file);

            // Delegate to actual upload strategy
            Map<String, String> result = uploadStrategy.uploadVideo(file, folder, userId);

            // Record in history
            recordUploadHistory(file.getOriginalFilename(), folder, "video", result, userId, startTime);

            totalUploads++;
            totalVideoUploads++;

            long duration = System.currentTimeMillis() - startTime;
            log.info("[PROXY] Video upload completed successfully in {} ms - URL: {}", 
                    duration, result.get("secure_url"));

            return result;

        } catch (IllegalArgumentException e) {
            totalUploadErrors++;
            log.error("[PROXY] Validation error during video upload: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            totalUploadErrors++;
            log.error("[PROXY] IOException during video upload: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            totalUploadErrors++;
            log.error("[PROXY] Unexpected error during video upload: {}", e.getMessage(), e);
            throw new RuntimeException("[PROXY] Unexpected error uploading video: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String publicId) throws IOException {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("[PROXY] Starting file deletion - PublicId: {}", publicId);

            boolean result = uploadStrategy.deleteFile(publicId);

            // Remove from cache
            uploadCache.values().removeIf(entry -> entry.getOrDefault("public_id", "").equals(publicId));

            long duration = System.currentTimeMillis() - startTime;
            log.info("[PROXY] File deletion completed in {} ms - Success: {}", duration, result);

            return result;

        } catch (IOException e) {
            log.error("[PROXY] IOException during file deletion: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("[PROXY] Unexpected error during file deletion: {}", e.getMessage(), e);
            throw new RuntimeException("[PROXY] Unexpected error deleting file: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStrategyName() {
        return "CLOUDINARY_WITH_PROXY";
    }

    /**
     * Get upload statistics
     */
    public Map<String, Object> getUploadStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUploads", totalUploads);
        stats.put("totalImageUploads", totalImageUploads);
        stats.put("totalVideoUploads", totalVideoUploads);
        stats.put("totalErrors", totalUploadErrors);
        stats.put("cacheSize", uploadCache.size());
        stats.put("historySize", uploadHistory.size());
        
        double successRate = totalUploads > 0 ? 
            ((double)(totalUploads - totalUploadErrors) / totalUploads * 100) : 0;
        stats.put("successRate", String.format("%.2f%%", successRate));
        
        return stats;
    }

    /**
     * Get upload history
     */
    public Map<String, UploadHistoryRecord> getUploadHistory() {
        return new HashMap<>(uploadHistory);
    }

    /**
     * Clear cache (useful for testing or memory management)
     */
    public void clearCache() {
        uploadCache.clear();
        log.info("[PROXY] Upload cache cleared");
    }

    /**
     * Clear upload history
     */
    public void clearHistory() {
        uploadHistory.clear();
        log.info("[PROXY] Upload history cleared");
    }

    // ==================== Private Helper Methods ====================

    private String generateCacheKey(MultipartFile file, String folder) {
        return folder + "_" + file.getOriginalFilename() + "_" + file.getSize();
    }

    private void recordUploadHistory(String fileName, String folder, String fileType, 
                                     Map<String, String> result, Integer userId, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        
        UploadHistoryRecord record = new UploadHistoryRecord(
            fileName, folder, fileType, result.get("secure_url"), 
            result.get("public_id"), userId, duration, true
        );
        
        uploadHistory.put(result.get("public_id"), record);
    }

    // ==================== Inner Classes ====================

    /**
     * Record for tracking upload history
     */
    public static class UploadHistoryRecord {
        private String fileName;
        private String folder;
        private String fileType;
        private String uploadedUrl;
        private String publicId;
        private Integer uploadedBy;
        private long durationMs;
        private boolean successful;
        private long timestamp;

        public UploadHistoryRecord(String fileName, String folder, String fileType, 
                                  String uploadedUrl, String publicId, Integer uploadedBy, 
                                  long durationMs, boolean successful) {
            this.fileName = fileName;
            this.folder = folder;
            this.fileType = fileType;
            this.uploadedUrl = uploadedUrl;
            this.publicId = publicId;
            this.uploadedBy = uploadedBy;
            this.durationMs = durationMs;
            this.successful = successful;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getFileName() { return fileName; }
        public String getFolder() { return folder; }
        public String getFileType() { return fileType; }
        public String getUploadedUrl() { return uploadedUrl; }
        public String getPublicId() { return publicId; }
        public Integer getUploadedBy() { return uploadedBy; }
        public long getDurationMs() { return durationMs; }
        public boolean isSuccessful() { return successful; }
        public long getTimestamp() { return timestamp; }
    }
}
