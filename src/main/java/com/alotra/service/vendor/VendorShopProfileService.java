package com.alotra.service.vendor;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.shop.ShopProfileDTO;
import com.alotra.entity.shop.Shop;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.service.cloudinary.CloudinaryService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorShopProfileService {

	private final ShopRepository shopRepository;
	private final CloudinaryService cloudinaryService;

	@PersistenceContext // Inject EntityManager for JPQL
	private EntityManager entityManager;
	// ==================== SHOP PROFILE MANAGEMENT ====================
	// Thêm vào VendorService.java

	@Transactional(readOnly = true)
	public ShopProfileDTO getShopProfile(Integer shopId) {
		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));

		ShopProfileDTO dto = new ShopProfileDTO();
		dto.setShopId(shop.getShopId());
		dto.setShopName(shop.getShopName());
		dto.setDescription(shop.getDescription());
		dto.setLogoURL(shop.getLogoURL());
		dto.setCoverImageURL(shop.getCoverImageURL());
		dto.setAddress(shop.getAddress());
		dto.setPhoneNumber(shop.getPhoneNumber());
		dto.setStatus(shop.getStatus());

		// Convert status to text
		switch (shop.getStatus()) {
		case 0:
			dto.setStatusText("Đang chờ duyệt");
			break;
		case 1:
			dto.setStatusText("Đang hoạt động");
			break;
		case 2:
			dto.setStatusText("Đã bị đình chỉ");
			break;
		default:
			dto.setStatusText("Không xác định");
		}

		dto.setCommissionRate(shop.getCommissionRate());
		dto.setCreatedAt(shop.getCreatedAt());
		dto.setUpdatedAt(shop.getUpdatedAt());

		// Owner info
		if (shop.getUser() != null) {
			dto.setOwnerName(shop.getUser().getFullName());
			dto.setOwnerEmail(shop.getUser().getEmail());
		}

		return dto;
	}

	@Transactional
	public void updateShopProfile(Integer shopId, ShopProfileDTO request, Integer userId) throws Exception {

		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));

		// Kiểm tra quyền sở hữu
		if (!shop.getUser().getId().equals(userId)) {
			throw new RuntimeException("Unauthorized: You are not the owner of this shop");
		}

		// Kiểm tra trùng tên shop (nếu đổi tên)
		if (!shop.getShopName().equals(request.getShopName())) {
			Optional<Shop> existingShop = shopRepository.findByShopName(request.getShopName());
			if (existingShop.isPresent() && !existingShop.get().getShopId().equals(shopId)) {
				throw new RuntimeException("Tên cửa hàng đã tồn tại");
			}
		}

		// Upload logo mới (nếu có)
		if (request.getLogoFile() != null && !request.getLogoFile().isEmpty()) {
			try {
				Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(request.getLogoFile(),
						"shops/logos", userId);
				String newLogoUrl = uploadResult.get("secure_url");
				if (newLogoUrl != null) {
					shop.setLogoURL(newLogoUrl);
					log.info("Uploaded new logo for shop {}: {}", shopId, newLogoUrl);
				}
			} catch (Exception e) {
				log.error("Error uploading logo: {}", e.getMessage(), e);
				throw new RuntimeException("Lỗi khi upload logo: " + e.getMessage());
			}
		}

		// Upload cover image mới (nếu có)
		if (request.getCoverImageFile() != null && !request.getCoverImageFile().isEmpty()) {
			try {
				Map<String, String> uploadResult = cloudinaryService
						.uploadImageAndReturnDetails(request.getCoverImageFile(), "shops/covers", userId);
				String newCoverUrl = uploadResult.get("secure_url");
				if (newCoverUrl != null) {
					shop.setCoverImageURL(newCoverUrl);
					log.info("Uploaded new cover image for shop {}: {}", shopId, newCoverUrl);
				}
			} catch (Exception e) {
				log.error("Error uploading cover image: {}", e.getMessage(), e);
				throw new RuntimeException("Lỗi khi upload ảnh bìa: " + e.getMessage());
			}
		}

		// Cập nhật thông tin cơ bản
		shop.setShopName(request.getShopName());
		shop.setDescription(request.getDescription());
		shop.setAddress(request.getAddress());
		shop.setPhoneNumber(request.getPhoneNumber());

		// updatedAt sẽ tự động cập nhật qua @PreUpdate

		shopRepository.save(shop);

		log.info("Shop profile updated successfully - Shop ID: {}, User ID: {}", shopId, userId);
	}

}
