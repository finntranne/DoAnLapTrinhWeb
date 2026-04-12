package com.alotra.service.cloudinary.strategy;

import com.alotra.entity.common.CloudinaryAsset;
import com.alotra.repository.common.CloudinaryAssetRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cloudinary Upload Strategy
 * Implements image/video upload to Cloudinary cloud storage
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryUploadStrategy implements UploadStrategy {

    private final Cloudinary cloudinary;
    private final CloudinaryAssetRepository assetRepository;

    @Override
    public Map<String, String> uploadImage(MultipartFile file, String folder, Integer userId) throws IOException {
        FileValidationUtil.validateImageFile(file);

        try {
            String publicId = generatePublicId(folder);
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "alotra/" + folder,
                    "resource_type", "image",
                    "overwrite", false,
                    "quality", "auto:good",
                    "fetch_format", "auto"
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            String imageUrl = (String) uploadResult.get("secure_url");
            String fullPublicId = (String) uploadResult.get("public_id");

            if (imageUrl == null || fullPublicId == null) {
                log.error("Cloudinary upload failed. Result: {}", uploadResult);
                throw new IOException("Cloudinary upload failed to return URL or Public ID.");
            }

            saveAssetRecord(fullPublicId, imageUrl, "image", userId);

            log.info("Image uploaded via Cloudinary successfully: URL={}, PublicID={}", imageUrl, fullPublicId);

            Map<String, String> result = new HashMap<>();
            result.put("public_id", fullPublicId);
            result.put("secure_url", imageUrl);
            return result;

        } catch (IOException e) {
            log.error("IOException during Cloudinary image upload: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Cloudinary image upload: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error uploading image: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> uploadVideo(MultipartFile file, String folder, Integer userId) throws IOException {
        FileValidationUtil.validateVideoFile(file);

        try {
            String publicId = generatePublicId(folder);
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "alotra/" + folder,
                    "resource_type", "video",
                    "overwrite", false
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            String videoUrl = (String) uploadResult.get("secure_url");
            String fullPublicId = (String) uploadResult.get("public_id");

            if (videoUrl == null || fullPublicId == null) {
                log.error("Cloudinary video upload failed. Result: {}", uploadResult);
                throw new IOException("Cloudinary video upload failed to return URL or Public ID.");
            }

            saveAssetRecord(fullPublicId, videoUrl, "video", userId);

            log.info("Video uploaded via Cloudinary successfully: URL={}, PublicID={}", videoUrl, fullPublicId);

            Map<String, String> result = new HashMap<>();
            result.put("public_id", fullPublicId);
            result.put("secure_url", videoUrl);
            return result;

        } catch (IOException e) {
            log.error("IOException during Cloudinary video upload: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Cloudinary video upload: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error uploading video: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String publicId) throws IOException {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
            String resultStatus = (String) result.get("result");

            if ("ok".equals(resultStatus)) {
                assetRepository.findByPublicId(publicId).ifPresent(asset -> {
                    assetRepository.delete(asset);
                    log.info("File deleted from Cloudinary: {}", publicId);
                });
                return true;
            }

            log.warn("Failed to delete file from Cloudinary: {}", publicId);
            return false;

        } catch (Exception e) {
            log.error("Error deleting file from Cloudinary: {}", publicId, e);
            throw new IOException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStrategyName() {
        return "CLOUDINARY";
    }

    private String generatePublicId(String folder) {
        return folder + "/" + UUID.randomUUID().toString();
    }

    private void saveAssetRecord(String publicId, String url, String resourceType, Integer userId) {
        CloudinaryAsset asset = new CloudinaryAsset();
        asset.setPublicId(publicId);
        asset.setCloudinaryUrl(url);
        asset.setResourceType(resourceType);

        if (userId != null) {
            // You can set user reference if needed
            // asset.setUser(userRepository.findById(userId).orElse(null));
        }

        assetRepository.save(asset);
        log.debug("Asset record saved: publicId={}, resourceType={}", publicId, resourceType);
    }
}
