package com.alotra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC Configuration
 * Configures static resource mapping for local file uploads
 * Maps URI path /uploads/* to file system folder uploads/
 */
@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get absolute path of uploads folder
        String uploadsPath = Paths.get("uploads").toAbsolutePath().toString();
        
        log.info("Configuring static resource handler");
        log.info("URI path: /uploads/**");
        log.info("File system path: file:///{}", uploadsPath);
        
        // Map /uploads/** to uploads folder
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + uploadsPath + "/")
                .setCachePeriod(3600);  // Cache for 1 hour
        
        log.info("✅ Static resource handler configured successfully");
    }
}
