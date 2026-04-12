package com.alotra.service.cloudinary.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Local File Upload Strategy
 * Implements image/video upload to local file system
 * Useful for development, testing, or as fallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalUploadStrategy implements UploadStrategy {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    @Override
    public Map<String, String> uploadImage(MultipartFile file, String folder, Integer userId) throws IOException {
        FileValidationUtil.validateImageFile(file);
        return performUpload(file, folder, userId);
    }

    @Override
    public Map<String, String> uploadVideo(MultipartFile file, String folder, Integer userId) throws IOException {
        FileValidationUtil.validateVideoFile(file);
        return performUpload(file, folder, userId);
    }

    @Override
    public boolean deleteFile(String publicId) throws IOException {
        try {
            String filePath = publicId;
            Path path = Paths.get(filePath);
            
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("File deleted locally: {}", publicId);
                return true;
            }
            
            log.warn("File not found for deletion: {}", publicId);
            return false;
            
        } catch (IOException e) {
            log.error("Error deleting file locally: {}", publicId, e);
            throw e;
        }
    }

    @Override
    public String getStrategyName() {
        return "LOCAL_STORAGE";
    }

    // ==================== Private Methods ====================

    private Map<String, String> performUpload(MultipartFile file, String folder, Integer userId) throws IOException {
        try {
            // Create directory structure
            String fullPath = uploadDir + File.separator + folder;
            Path dirPath = Paths.get(fullPath);
            Files.createDirectories(dirPath);

            // Generate unique filename
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = dirPath.resolve(fileName);

            // Save file
            Files.write(filePath, file.getBytes());

            // Generate public ID and URL
            String publicId = filePath.toString();
            String fileUrl = baseUrl + "/" + folder + "/" + fileName;

            log.info("File uploaded locally - Name: {}, Path: {}, URL: {}", 
                    fileName, filePath, fileUrl);

            Map<String, String> result = new HashMap<>();
            result.put("public_id", publicId);
            result.put("secure_url", fileUrl);
            result.put("file_name", fileName);
            result.put("folder", folder);

            return result;

        } catch (IOException e) {
            log.error("Error uploading file locally: {}", e.getMessage(), e);
            throw e;
        }
    }
}
