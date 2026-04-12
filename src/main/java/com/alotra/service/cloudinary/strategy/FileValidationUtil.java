package com.alotra.service.cloudinary.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * Utility class for file validation
 * Centralizes all validation logic for images and videos
 * Eliminates code duplication across Strategy implementations
 * 
 * Follows Single Responsibility Principle (SRP)
 */
@Slf4j
public class FileValidationUtil {

    // File size limits
    private static final long IMAGE_SIZE_LIMIT = 10 * 1024 * 1024;  // 10MB
    private static final long VIDEO_SIZE_LIMIT = 100 * 1024 * 1024; // 100MB

    /**
     * Validate image file
     * Checks: not null, not empty, correct content type, file size
     *
     * @param file The image file to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateImageFile(MultipartFile file) {
        validateFileNotEmpty(file);
        validateImageContentType(file);
        validateImageFileSize(file);
        log.debug("Image file validation passed - Name: {}, Size: {} bytes", 
                file.getOriginalFilename(), file.getSize());
    }

    /**
     * Validate video file
     * Checks: not null, not empty, correct content type, file size
     *
     * @param file The video file to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateVideoFile(MultipartFile file) {
        validateFileNotEmpty(file);
        validateVideoContentType(file);
        validateVideoFileSize(file);
        log.debug("Video file validation passed - Name: {}, Size: {} bytes", 
                file.getOriginalFilename(), file.getSize());
    }

    /**
     * Validate that file is not null and not empty
     */
    private static void validateFileNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
    }

    /**
     * Validate that content type is image
     */
    private static void validateImageContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "File must be an image. Received content type: " + contentType);
        }
    }

    /**
     * Validate that content type is video
     */
    private static void validateVideoContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException(
                    "File must be a video. Received content type: " + contentType);
        }
    }

    /**
     * Validate image file size does not exceed limit (10MB)
     */
    private static void validateImageFileSize(MultipartFile file) {
        if (file.getSize() > IMAGE_SIZE_LIMIT) {
            throw new IllegalArgumentException(
                    String.format("Image file size (%d MB) exceeds 10MB limit",
                            file.getSize() / (1024 * 1024)));
        }
    }

    /**
     * Validate video file size does not exceed limit (100MB)
     */
    private static void validateVideoFileSize(MultipartFile file) {
        if (file.getSize() > VIDEO_SIZE_LIMIT) {
            throw new IllegalArgumentException(
                    String.format("Video file size (%d MB) exceeds 100MB limit",
                            file.getSize() / (1024 * 1024)));
        }
    }

    /**
     * Get image size limit in bytes
     */
    public static long getImageSizeLimit() {
        return IMAGE_SIZE_LIMIT;
    }

    /**
     * Get video size limit in bytes
     */
    public static long getVideoSizeLimit() {
        return VIDEO_SIZE_LIMIT;
    }
}
