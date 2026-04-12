package com.alotra.service.cloudinary.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Upload Facade Service
 * Provides a clean interface for file uploads using Strategy Pattern + Proxy Pattern
 * This service acts as a wrapper around UploadStrategyContext
 * 
 * Usage:
 *   - Inject this service into controllers/services
 *   - Call uploadImage(), uploadVideo(), deleteFile() methods
 *   - Optionally switch strategies at runtime
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final UploadStrategyContext strategyContext;

    /**
     * Upload an image file
     *
     * @param file   MultipartFile to upload
     * @param folder Destination folder (e.g., "avatars", "products")
     * @param userId User ID who uploaded (optional)
     * @return Map with "public_id" and "secure_url"
     */
    public Map<String, String> uploadImage(MultipartFile file, String folder, Integer userId) throws IOException {
        log.info("UploadService: uploading image to folder '{}' for user {}", folder, userId);
        return strategyContext.uploadImage(file, folder, userId);
    }

    /**
     * Upload an image file without user tracking
     */
    public Map<String, String> uploadImage(MultipartFile file, String folder) throws IOException {
        return uploadImage(file, folder, null);
    }

    /**
     * Upload a video file
     *
     * @param file   MultipartFile to upload
     * @param folder Destination folder (e.g., "reviews")
     * @param userId User ID who uploaded (optional)
     * @return Map with "public_id" and "secure_url"
     */
    public Map<String, String> uploadVideo(MultipartFile file, String folder, Integer userId) throws IOException {
        log.info("UploadService: uploading video to folder '{}' for user {}", folder, userId);
        return strategyContext.uploadVideo(file, folder, userId);
    }

    /**
     * Upload a video file without user tracking
     */
    public Map<String, String> uploadVideo(MultipartFile file, String folder) throws IOException {
        return uploadVideo(file, folder, null);
    }

    /**
     * Delete a file
     *
     * @param publicId Public ID of the resource to delete
     * @return true if deletion was successful
     */
    public boolean deleteFile(String publicId) throws IOException {
        log.info("UploadService: deleting file with public ID '{}'", publicId);
        return strategyContext.deleteFile(publicId);
    }

    /**
     * Change upload strategy at runtime
     *
     * @param strategyName "CLOUDINARY", "CLOUDINARY_WITH_PROXY", "LOCAL_STORAGE"
     */
    public void setUploadStrategy(String strategyName) {
        log.info("UploadService: changing upload strategy to '{}'", strategyName);
        strategyContext.setStrategyByName(strategyName);
    }

    /**
     * Get current upload strategy name
     */
    public String getCurrentStrategy() {
        return strategyContext.getCurrentStrategyName();
    }

    /**
     * Get upload statistics (only available with Proxy strategy)
     */
    public Map<String, Object> getStatistics() {
        return strategyContext.getUploadStatistics();
    }

    /**
     * Get upload history (only available with Proxy strategy)
     */
    public Map<String, CloudinaryServiceProxy.UploadHistoryRecord> getHistory() {
        return strategyContext.getUploadHistory();
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        strategyContext.clearCache();
    }

    /**
     * Clear history
     */
    public void clearHistory() {
        strategyContext.clearHistory();
    }
}
