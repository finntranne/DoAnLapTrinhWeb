package com.alotra.service.cloudinary;

import com.alotra.entity.common.CloudinaryAsset;
import com.alotra.entity.user.User;
import com.alotra.repository.common.CloudinaryAssetRepository;
import com.alotra.repository.user.UserRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CloudinaryService {

	private final CloudinaryAssetRepository assetRepository;
	private final UserRepository userRepository;

	@Value("${cloudinary.cloud-name}")
	private String cloudName;

	@Value("${cloudinary.api-key}")
	private String apiKey;

	@Value("${cloudinary.api-secret}")
	private String apiSecret;

	private Cloudinary cloudinary;

	@PostConstruct
	public void init() {
		cloudinary = new Cloudinary(
				ObjectUtils.asMap("cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret, "secure", true));
		log.info("Cloudinary initialized successfully");
	}

	/**
	 * Upload image to Cloudinary
	 * 
	 * @param file MultipartFile to upload
	 * @return URL of uploaded image
	 */
	public String uploadImage(MultipartFile file) {
		return uploadImage(file, "general", null);
	}

	/**
	 * Upload image to Cloudinary with folder
	 * 
	 * @param file   MultipartFile to upload
	 * @param folder Folder name in Cloudinary
	 * @return URL of uploaded image
	 */
	public String uploadImage(MultipartFile file, String folder) {
		return uploadImage(file, folder, null);
	}

	/**
	 * Upload image to Cloudinary with folder and user tracking
	 * 
	 * @param file   MultipartFile to upload
	 * @param folder Folder name in Cloudinary
	 * @param userId User ID who uploaded
	 * @return URL of uploaded image
	 */
	public String uploadImage(MultipartFile file, String folder, Integer userId) {
		validateImageFile(file);

		try {
			// Generate unique public ID
			String publicId = generatePublicId(folder);

			// Upload to Cloudinary
			Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
					ObjectUtils.asMap("public_id", publicId, "folder", "alotra/" + folder, "resource_type", "image",
							"overwrite", false, "quality", "auto:good", "fetch_format", "auto"));

			String imageUrl = (String) uploadResult.get("secure_url");
			String fullPublicId = (String) uploadResult.get("public_id");

			// Save to database
			saveAssetRecord(fullPublicId, imageUrl, "image", userId);

			log.info("Image uploaded successfully: {}", imageUrl);
			return imageUrl;

		} catch (IOException e) {
			log.error("Error uploading image to Cloudinary", e);
			throw new RuntimeException("Failed to upload image: " + e.getMessage());
		}
	}

	/**
	 * Upload image to Cloudinary, save asset record, and return details.
	 *
	 * @param file   MultipartFile to upload
	 * @param folder Folder name in Cloudinary (e.g., "products", "avatars")
	 * @param userId Optional User ID who uploaded
	 * @return Map containing "public_id" and "secure_url"
	 * @throws IOException              If upload fails
	 * @throws IllegalArgumentException If file validation fails
	 */
	public Map<String, String> uploadImageAndReturnDetails(MultipartFile file, String folder, Integer userId)
			throws IOException {
		validateImageFile(file); // Reuse existing validation

		String publicId = generatePublicId(folder); // Reuse public ID generation

		// Upload parameters (same as before)
		Map<String, Object> uploadParams = ObjectUtils.asMap("public_id", publicId, "folder", "alotra/" + folder,
				"resource_type", "image", "overwrite", false, "quality", "auto:good", "fetch_format", "auto");

		try {
			// Perform the upload
			Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

			// Extract results from Cloudinary's response
			String imageUrl = (String) uploadResult.get("secure_url");
			String fullPublicId = (String) uploadResult.get("public_id"); // This includes the folder path

			if (imageUrl == null || fullPublicId == null) {
				log.error("Cloudinary upload failed or did not return expected values. Result: {}", uploadResult);
				throw new IOException("Cloudinary upload failed to return URL or Public ID.");
			}

			// Save asset record to your database
			saveAssetRecord(fullPublicId, imageUrl, "image", userId); // Reuse saving logic

			log.info("Image uploaded successfully: URL={}, PublicID={}", imageUrl, fullPublicId);

			// Prepare the result map to return
			Map<String, String> resultDetails = new HashMap<>();
			resultDetails.put("public_id", fullPublicId);
			resultDetails.put("secure_url", imageUrl);

			return resultDetails;

		} catch (IOException e) {
			log.error("IOException during Cloudinary upload for publicId base {}: {}", publicId, e.getMessage(), e);
			throw e; // Re-throw IOException so the calling service can handle it
		} catch (Exception e) {
			// Catch other potential runtime errors during upload
			log.error("Unexpected error during Cloudinary upload for publicId base {}: {}", publicId, e.getMessage(),
					e);
			throw new RuntimeException("Unexpected error uploading image: " + e.getMessage(), e);
		}
	}

	/**
	 * Upload video to Cloudinary
	 */
	public String uploadVideo(MultipartFile file, String folder, Integer userId) {
		validateVideoFile(file);

		try {
			String publicId = generatePublicId(folder);

			Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("public_id", publicId,
					"folder", "alotra/" + folder, "resource_type", "video", "overwrite", false));

			String videoUrl = (String) uploadResult.get("secure_url");
			String fullPublicId = (String) uploadResult.get("public_id");

			saveAssetRecord(fullPublicId, videoUrl, "video", userId);

			log.info("Video uploaded successfully: {}", videoUrl);
			return videoUrl;

		} catch (IOException e) {
			log.error("Error uploading video to Cloudinary", e);
			throw new RuntimeException("Failed to upload video: " + e.getMessage());
		}
	}

	/**
	 * Delete image from Cloudinary
	 */
	public void deleteImage(String publicId) {
		try {
			Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));

			String resultStatus = (String) result.get("result");
			if ("ok".equals(resultStatus)) {
				// Remove from database
				assetRepository.findByPublicId(publicId).ifPresent(asset -> {
					assetRepository.delete(asset);
					log.info("Image deleted from database: {}", publicId);
				});
				log.info("Image deleted from Cloudinary: {}", publicId);
			} else {
				log.warn("Failed to delete image from Cloudinary: {}", publicId);
			}

		} catch (IOException e) {
			log.error("Error deleting image from Cloudinary", e);
			throw new RuntimeException("Failed to delete image: " + e.getMessage());
		}
	}

	/**
	 * Delete video from Cloudinary
	 */
	public void deleteVideo(String publicId) {
		try {
			Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));

			String resultStatus = (String) result.get("result");
			if ("ok".equals(resultStatus)) {
				assetRepository.findByPublicId(publicId).ifPresent(assetRepository::delete);
				log.info("Video deleted from Cloudinary: {}", publicId);
			}

		} catch (IOException e) {
			log.error("Error deleting video from Cloudinary", e);
			throw new RuntimeException("Failed to delete video: " + e.getMessage());
		}
	}

	/**
	 * Get image URL with transformation
	 */
	public String getTransformedImageUrl(String publicId, int width, int height) {
		return cloudinary.url().transformation(new Transformation<>().width(width).height(height).crop("fill")
				.gravity("auto").quality("auto").fetchFormat("auto")).generate(publicId);
	}

	/**
	 * Get thumbnail URL
	 */
	public String getThumbnailUrl(String publicId) {
		return getTransformedImageUrl(publicId, 200, 200);
	}

	// ============================================
	// HELPER METHODS
	// ============================================

	private void validateImageFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File cannot be empty");
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new IllegalArgumentException("File must be an image");
		}

		// Check file size (max 10MB)
		long maxSize = 10 * 1024 * 1024; // 10MB
		if (file.getSize() > maxSize) {
			throw new IllegalArgumentException("File size must not exceed 10MB");
		}
	}

	private void validateVideoFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File cannot be empty");
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("video/")) {
			throw new IllegalArgumentException("File must be a video");
		}

		// Check file size (max 100MB)
		long maxSize = 100 * 1024 * 1024; // 100MB
		if (file.getSize() > maxSize) {
			throw new IllegalArgumentException("Video size must not exceed 100MB");
		}
	}

	private String generatePublicId(String folder) {
		return folder + "_" + UUID.randomUUID().toString().replace("-", "");
	}

	private void saveAssetRecord(String publicId, String url, String resourceType, Integer userId) {
		CloudinaryAsset asset = new CloudinaryAsset();
		asset.setPublicId(publicId);
		asset.setCloudinaryUrl(url);
		asset.setResourceType(resourceType);

		if (userId != null) {
			userRepository.findById(userId).ifPresent(asset::setUploadedBy);
		}

		assetRepository.save(asset);
	}

	/**
	 * Extract public ID from Cloudinary URL
	 */
	public String extractPublicIdFromUrl(String url) {
		if (url == null || url.isEmpty()) {
			return null;
		}

		try {
			// URL format:
			// https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{format}
			String[] parts = url.split("/upload/");
			if (parts.length < 2) {
				return null;
			}

			String publicIdPart = parts[1];
			// Remove version prefix (v1234567890/)
			publicIdPart = publicIdPart.replaceFirst("v\\d+/", "");
			// Remove file extension
			int dotIndex = publicIdPart.lastIndexOf(".");
			if (dotIndex > 0) {
				publicIdPart = publicIdPart.substring(0, dotIndex);
			}

			return publicIdPart;
		} catch (Exception e) {
			log.error("Error extracting public ID from URL: {}", url, e);
			return null;
		}
	}

	/**
	 * Cleanup old unused assets (scheduled task)
	 */
	public void cleanupOldAssets(int daysOld) {
		// This should be called by a scheduled task
		// Delete assets older than specified days that are not referenced
		log.info("Cleanup old assets older than {} days", daysOld);
		// Implementation depends on how you track asset usage
	}
}