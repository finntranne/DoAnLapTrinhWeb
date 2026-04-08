package com.alotra.config;

import com.alotra.service.cloudinary.strategy.UploadStrategyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Upload Strategy Configuration
 * Initializes the upload strategy on application startup
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class UploadStrategyConfig {

    private final UploadStrategyContext strategyContext;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeUploadStrategy() {
        log.info("=== Upload Strategy Configuration ===");
        strategyContext.initialize();
        log.info("Default strategy: {}", strategyContext.getCurrentStrategyName());
        log.info("Upload system ready with Strategy Pattern + Proxy Pattern");
        log.info("=====================================");
    }
}
