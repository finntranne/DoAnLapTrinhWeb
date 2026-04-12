package com.alotra.controller.api;

import com.alotra.service.cloudinary.strategy.UploadService;
import com.alotra.service.cloudinary.strategy.CloudinaryServiceProxy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Upload API Controller
 * Demonstrates how to use Strategy Pattern + Proxy Pattern in a REST API
 * 
 * Endpoints:
 * - POST /api/upload/image - Upload image file
 * - POST /api/upload/video - Upload video file
 * - DELETE /api/upload/{publicId} - Delete uploaded file
 * - GET /api/upload/statistics - Get upload statistics
 * - GET /api/upload/history - Get upload history
 * - POST /api/upload/strategy - Change upload strategy
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadApiController {

    private final UploadService uploadService;

    /**
     * Upload an image file
     * 
     * Example usage:
     * curl -X POST http://localhost:8080/api/upload/image \
     *   -F "file=@avatar.jpg" \
     *   -F "folder=avatars" \
     *   -F "userId=123"
     */
    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "general") String folder,
            @RequestParam(required = false) Integer userId) {

        try {
            log.info("Uploading image: {}, folder: {}, userId: {}", file.getOriginalFilename(), folder, userId);

            Map<String, String> result = uploadService.uploadImage(file, folder, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("publicId", result.get("public_id"));
            response.put("url", result.get("secure_url"));
            response.put("strategy", uploadService.getCurrentStrategy());
            response.put("message", "Image uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Validation Error",
                    "message", e.getMessage()
            ));
        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Upload Failed",
                    "message", "Failed to upload image: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Internal Server Error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Upload a video file
     * 
     * Example usage:
     * curl -X POST http://localhost:8080/api/upload/video \
     *   -F "file=@review.mp4" \
     *   -F "folder=reviews" \
     *   -F "userId=456"
     */
    @PostMapping("/video")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "general") String folder,
            @RequestParam(required = false) Integer userId) {

        try {
            log.info("Uploading video: {}, folder: {}, userId: {}", file.getOriginalFilename(), folder, userId);

            Map<String, String> result = uploadService.uploadVideo(file, folder, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("publicId", result.get("public_id"));
            response.put("url", result.get("secure_url"));
            response.put("strategy", uploadService.getCurrentStrategy());
            response.put("message", "Video uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Validation Error",
                    "message", e.getMessage()
            ));
        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Upload Failed",
                    "message", "Failed to upload video: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete an uploaded file
     * 
     * Example usage:
     * curl -X DELETE http://localhost:8080/api/upload/alotra%2Favatars%2Fab12c345
     */
    @DeleteMapping("/{publicId}")
    public ResponseEntity<?> deleteFile(@PathVariable String publicId) {
        try {
            log.info("Deleting file: {}", publicId);

            boolean success = uploadService.deleteFile(publicId);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "File deleted successfully",
                        "publicId", publicId
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Deletion Failed",
                        "message", "File could not be deleted"
                ));
            }

        } catch (IOException e) {
            log.error("Deletion failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Internal Server Error",
                    "message", "Failed to delete file: " + e.getMessage()
            ));
        }
    }

    /**
     * Get upload statistics (Proxy strategy only)
     * 
     * Example usage:
     * curl http://localhost:8080/api/upload/statistics
     * 
     * Response:
     * {
     *   "totalUploads": 450,
     *   "totalImageUploads": 380,
     *   "totalVideoUploads": 70,
     *   "totalErrors": 12,
     *   "cacheSize": 89,
     *   "historySize": 450,
     *   "successRate": "97.33%"
     * }
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> stats = uploadService.getStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("strategy", uploadService.getCurrentStrategy());
            response.put("statistics", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get statistics: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to retrieve statistics",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get upload history (Proxy strategy only)
     * Shows all uploads with timestamps, durations, and status
     * 
     * Example usage:
     * curl http://localhost:8080/api/upload/history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getUploadHistory() {
        try {
            Map<String, CloudinaryServiceProxy.UploadHistoryRecord> history = uploadService.getHistory();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("strategy", uploadService.getCurrentStrategy());
            response.put("totalRecords", history.size());
            response.put("history", history);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get history: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to retrieve history",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Change upload strategy at runtime
     * 
     * Example usage:
     * curl -X POST http://localhost:8080/api/upload/strategy \
     *   -H "Content-Type: application/json" \
     *   -d '{"strategy": "LOCAL_STORAGE"}'
     * 
     * Available strategies:
     * - CLOUDINARY: Direct upload to Cloudinary (no features)
     * - CLOUDINARY_WITH_PROXY: With caching, logging, history (recommended)
     * - LOCAL_STORAGE: Upload to local file system (dev/testing)
     */
    @PostMapping("/strategy")
    public ResponseEntity<?> changeStrategy(@RequestBody Map<String, String> request) {
        try {
            String strategyName = request.get("strategy");
            
            if (strategyName == null || strategyName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Strategy name required",
                        "availableStrategies", new String[]{
                                "CLOUDINARY",
                                "CLOUDINARY_WITH_PROXY",
                                "LOCAL_STORAGE"
                        }
                ));
            }

            uploadService.setUploadStrategy(strategyName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("currentStrategy", uploadService.getCurrentStrategy());
            response.put("message", "Upload strategy changed to " + strategyName);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to change strategy: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to change strategy",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Clear upload cache
     * Useful when you need to force re-upload of cached files
     * 
     * Example usage:
     * curl -X POST http://localhost:8080/api/upload/cache/clear
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearCache() {
        try {
            uploadService.clearCache();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Upload cache cleared successfully"
            ));

        } catch (Exception e) {
            log.error("Failed to clear cache: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to clear cache",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Clear upload history
     * Useful for privacy or when resetting statistics
     * 
     * Example usage:
     * curl -X POST http://localhost:8080/api/upload/history/clear
     */
    @PostMapping("/history/clear")
    public ResponseEntity<?> clearHistory() {
        try {
            uploadService.clearHistory();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Upload history cleared successfully"
            ));

        } catch (Exception e) {
            log.error("Failed to clear history: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to clear history",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get current upload configuration
     * 
     * Example usage:
     * curl http://localhost:8080/api/upload/config
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfiguration() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("currentStrategy", uploadService.getCurrentStrategy());
            config.put("availableStrategies", new String[]{
                    "CLOUDINARY",
                    "CLOUDINARY_WITH_PROXY",
                    "LOCAL_STORAGE"
            });
            config.put("imageSizeLimit", "10MB");
            config.put("videoSizeLimit", "100MB");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "configuration", config
            ));

        } catch (Exception e) {
            log.error("Failed to get configuration: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to get configuration",
                    "message", e.getMessage()
            ));
        }
    }
}
