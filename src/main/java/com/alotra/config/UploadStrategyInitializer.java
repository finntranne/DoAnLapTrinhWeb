package com.alotra.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alotra.service.cloudinary.strategy.UploadStrategyContext;

import jakarta.annotation.PostConstruct;

/**
 * Upload Strategy Initializer
 * Initialize upload strategy based on application.properties configuration
 * Allows switching between Cloudinary and Local storage
 * 
 * Configuration: upload.strategy in application.properties
 * Values:
 *   - CLOUDINARY: Direct Cloudinary API
 *   - CLOUDINARY_WITH_PROXY: Cloudinary with caching & history (default)
 *   - LOCAL_STORAGE: Save to local uploads/ folder
 * 
 * Example in application.properties:
 *   upload.strategy=LOCAL_STORAGE
 *   upload.strategy=CLOUDINARY
 *   upload.strategy=CLOUDINARY_WITH_PROXY (default)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UploadStrategyInitializer {

    private final UploadStrategyContext strategyContext;

    @Value("${upload.strategy:CLOUDINARY_WITH_PROXY}")
    private String uploadStrategy;

    @PostConstruct
    public void initializeUploadStrategy() {
        log.info("Initializing upload strategy: {}", uploadStrategy);
        
        try {
            strategyContext.setStrategyByName(uploadStrategy);
            String currentStrategy = strategyContext.getCurrentStrategyName();
            log.info("✅ Upload strategy initialized successfully: {}", currentStrategy);
            
            if ("LOCAL_STORAGE".equalsIgnoreCase(uploadStrategy)) {
                log.warn("⚠️  Using LOCAL_STORAGE strategy. Make sure /uploads/** is properly exposed in MvcConfig!");
                log.info("📁 Upload directory: uploads/");
                log.info("🔗 Base URL: http://localhost:8080/uploads");
            } else {
                log.info("☁️  Using Cloudinary strategy");
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize upload strategy: {}", uploadStrategy, e);
            log.warn("⚠️  Falling back to default strategy: CLOUDINARY_WITH_PROXY");
            strategyContext.setStrategyByName("CLOUDINARY_WITH_PROXY");
        }
    }
}
