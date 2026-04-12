package com.alotra.service.cloudinary.strategy;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

/**
 * Strategy interface for file upload operations.
 * Defines contract for different upload implementations (Cloudinary, Local, S3, etc.)
 */
public interface UploadStrategy {

    /**
     * Upload an image file
     *
     * @param file   MultipartFile to upload
     * @param folder Destination folder
     * @param userId User ID who is uploading (can be null)
     * @return Map containing upload details (public_id, secure_url, etc.)
     * @throws IOException If upload fails
     */
    Map<String, String> uploadImage(MultipartFile file, String folder, Integer userId) throws IOException;

    /**
     * Upload a video file
     *
     * @param file   MultipartFile to upload
     * @param folder Destination folder
     * @param userId User ID who is uploading (can be null)
     * @return Map containing upload details (public_id, secure_url, etc.)
     * @throws IOException If upload fails
     */
    Map<String, String> uploadVideo(MultipartFile file, String folder, Integer userId) throws IOException;

    /**
     * Delete a file
     *
     * @param publicId Public ID of the resource to delete
     * @return true if deletion was successful, false otherwise
     * @throws IOException If deletion fails
     */
    boolean deleteFile(String publicId) throws IOException;

    /**
     * Get strategy name
     *
     * @return Name of the upload strategy
     */
    String getStrategyName();
}
