package com.alotra.service.cloudinary.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Upload Strategy Context
 * Manages different upload strategies and provides a unified interface
 * Follows Strategy Pattern to switch between upload implementations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UploadStrategyContext {

    private final CloudinaryUploadStrategy cloudinaryStrategy;
    private final CloudinaryServiceProxy cloudinaryProxy;
    private final LocalUploadStrategy localStrategy;
    
    // Default strategy
    private UploadStrategy currentStrategy;

    /**
     * Initialize with default strategy (Cloudinary with Proxy)
     */
    public void initialize() {
        setStrategy(cloudinaryProxy);
        log.info("Upload strategy initialized with: {}", currentStrategy.getStrategyName());
    }

    /**
     * Set the current upload strategy
     *
     * @param strategy The strategy to use
     */
    public void setStrategy(UploadStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Upload strategy cannot be null");
        }
        this.currentStrategy = strategy;
        log.info("Upload strategy changed to: {}", strategy.getStrategyName());
    }

    /**
     * Set strategy by name
     *
     * @param strategyName "CLOUDINARY", "CLOUDINARY_WITH_PROXY", "LOCAL_STORAGE"
     */
    public void setStrategyByName(String strategyName) {
        switch (strategyName.toUpperCase()) {
            case "CLOUDINARY":
                setStrategy(cloudinaryStrategy);
                break;
            case "CLOUDINARY_WITH_PROXY":
                setStrategy(cloudinaryProxy);
                break;
            case "LOCAL_STORAGE":
                setStrategy(localStrategy);
                break;
            default:
                log.warn("Unknown strategy: {}. Using default CLOUDINARY_WITH_PROXY", strategyName);
                setStrategy(cloudinaryProxy);
        }
    }

    /**
     * Execute image upload using current strategy
     */
    public Map<String, String> uploadImage(MultipartFile file, String folder, Integer userId) throws IOException {
        if (currentStrategy == null) {
            initialize();
        }
        
        log.debug("Executing image upload with strategy: {}", currentStrategy.getStrategyName());
        return currentStrategy.uploadImage(file, folder, userId);
    }

    /**
     * Execute image upload without user tracking
     */
    public Map<String, String> uploadImage(MultipartFile file, String folder) throws IOException {
        return uploadImage(file, folder, null);
    }

    /**
     * Execute video upload using current strategy
     */
    public Map<String, String> uploadVideo(MultipartFile file, String folder, Integer userId) throws IOException {
        if (currentStrategy == null) {
            initialize();
        }
        
        log.debug("Executing video upload with strategy: {}", currentStrategy.getStrategyName());
        return currentStrategy.uploadVideo(file, folder, userId);
    }

    /**
     * Execute video upload without user tracking
     */
    public Map<String, String> uploadVideo(MultipartFile file, String folder) throws IOException {
        return uploadVideo(file, folder, null);
    }

    /**
     * Execute file deletion using current strategy
     */
    public boolean deleteFile(String publicId) throws IOException {
        if (currentStrategy == null) {
            initialize();
        }
        
        log.debug("Executing file deletion with strategy: {}", currentStrategy.getStrategyName());
        return currentStrategy.deleteFile(publicId);
    }

    /**
     * Get current strategy name
     */
    public String getCurrentStrategyName() {
        if (currentStrategy == null) {
            initialize();
        }
        return currentStrategy.getStrategyName();
    }

    /**
     * Get current strategy
     */
    public UploadStrategy getCurrentStrategy() {
        if (currentStrategy == null) {
            initialize();
        }
        return currentStrategy;
    }

    /**
     * Get upload statistics (if using Proxy strategy)
     */
    public Map<String, Object> getUploadStatistics() {
        if (currentStrategy instanceof CloudinaryServiceProxy) {
            return ((CloudinaryServiceProxy) currentStrategy).getUploadStatistics();
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("message", "Statistics not available for current strategy: " + getCurrentStrategyName());
        return stats;
    }

    /**
     * Get upload history (if using Proxy strategy)
     */
    public Map<String, CloudinaryServiceProxy.UploadHistoryRecord> getUploadHistory() {
        if (currentStrategy instanceof CloudinaryServiceProxy) {
            return ((CloudinaryServiceProxy) currentStrategy).getUploadHistory();
        }
        
        return new HashMap<>();
    }

    /**
     * Clear cache (if using Proxy strategy)
     */
    public void clearCache() {
        if (currentStrategy instanceof CloudinaryServiceProxy) {
            ((CloudinaryServiceProxy) currentStrategy).clearCache();
            log.info("Cache cleared");
        }
    }

    /**
     * Clear history (if using Proxy strategy)
     */
    public void clearHistory() {
        if (currentStrategy instanceof CloudinaryServiceProxy) {
            ((CloudinaryServiceProxy) currentStrategy).clearHistory();
            log.info("Upload history cleared");
        }
    }
}
