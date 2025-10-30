package com.alotra.service.vendor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.product.SimpleProductDTO;
import com.alotra.dto.promotion.ProductDiscountDTO;
import com.alotra.dto.promotion.PromotionRequestDTO;
import com.alotra.dto.promotion.PromotionStatisticsDTO;
import com.alotra.entity.product.Product;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.promotion.PromotionApproval;
import com.alotra.entity.promotion.PromotionProduct;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.promotion.PromotionApprovalRepository;
import com.alotra.repository.promotion.PromotionProductRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.notification.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorPromotionService {
	private final ShopRepository shopRepository;
	private final ProductRepository productRepository;
	private final PromotionRepository promotionRepository;
	private final PromotionProductRepository promotionProductRepository;
	private final PromotionApprovalRepository promotionApprovalRepository;
	private final NotificationService notificationService;
	private final UserRepository userRepository;

	private final ObjectMapper objectMapper;
	@PersistenceContext
	private EntityManager entityManager;

	public Page<PromotionStatisticsDTO> getShopPromotions(Integer shopId, Byte status, String promotionType,
			Pageable pageable) {

		LocalDateTime now = LocalDateTime.now();
		Page<Promotion> promotions = promotionRepository.findShopPromotionsFiltered(shopId, status, promotionType, now,
				pageable);

		return promotions.map(promotion -> {
			String approvalStatus = null;
			String activityStatus;

			Optional<PromotionApproval> latestApprovalOpt = promotionApprovalRepository
					.findTopByPromotion_PromotionIdOrderByRequestedAtDesc(promotion.getPromotionId());

			if (latestApprovalOpt.isPresent()) {
				PromotionApproval latestApproval = latestApprovalOpt.get();
				String currentDbStatus = latestApproval.getStatus();

				if ("Pending".equals(currentDbStatus) || "Rejected".equals(currentDbStatus)) {
					String actionTypeText = "";
					switch (latestApproval.getActionType()) {
					case "CREATE":
						actionTypeText = "Tạo mới";
						break;
					case "UPDATE":
						actionTypeText = "Cập nhật";
						break;
					case "DELETE":
						actionTypeText = "Xóa";
						break;
					default:
						actionTypeText = latestApproval.getActionType();
					}

					if ("Pending".equals(currentDbStatus)) {
						approvalStatus = "Đang chờ: " + actionTypeText;
					} else {
						approvalStatus = "Bị từ chối: " + actionTypeText;
					}
				}
			}

			if (promotion.getStatus() == 0) {
				activityStatus = "Không hoạt động";
			} else if (promotion.getEndDate().isBefore(now)) {
				activityStatus = "Đã kết thúc";
			} else {
				activityStatus = "Đang hoạt động";
			}

			return new PromotionStatisticsDTO(promotion, approvalStatus, activityStatus);
		});
	}

	public Promotion getPromotionDetail(Integer shopId, Integer promotionId) {
		Promotion promotion = promotionRepository.findById(promotionId)
				.orElseThrow(() -> new RuntimeException("Promotion not found"));

		if (!promotion.getCreatedByShopID().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Promotion does not belong to this shop");
		}

		return promotion;
	}

	public PromotionRequestDTO convertPromotionToDTO(Promotion promotion) {
		PromotionRequestDTO dto = new PromotionRequestDTO();

		dto.setPromotionId(promotion.getPromotionId());
		dto.setPromotionName(promotion.getPromotionName());
		dto.setDescription(promotion.getDescription());
		dto.setPromoCode(promotion.getPromoCode());
		dto.setDiscountType(promotion.getDiscountType());
		dto.setDiscountValue(promotion.getDiscountValue());
		dto.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
		dto.setMinOrderValue(promotion.getMinOrderValue());
		dto.setStartDate(promotion.getStartDate());
		dto.setEndDate(promotion.getEndDate());
		dto.setUsageLimit(promotion.getUsageLimit());

		return dto;
	}

	public PromotionRequestDTO convertPromotionToDTOWithProducts(Promotion promotion) {
		PromotionRequestDTO dto = convertPromotionToDTO(promotion);

		if ("PRODUCT".equals(promotion.getPromotionType())) {
			List<PromotionProduct> promoProducts = promotionProductRepository.findByPromotion(promotion);

			if (promoProducts != null && !promoProducts.isEmpty()) {
				List<ProductDiscountDTO> productDiscounts = promoProducts.stream().map(pp -> {
					ProductDiscountDTO pdDTO = new ProductDiscountDTO();
					pdDTO.setProductId(pp.getProduct().getProductID());
					pdDTO.setDiscountPercentage(pp.getDiscountPercentage());
					return pdDTO;
				}).collect(Collectors.toList());
				dto.setProductDiscounts(productDiscounts);
			}
		}
		return dto;
	}

	@Transactional
	public void requestPromotionCreation(Integer shopId, PromotionRequestDTO request, Integer userId)
			throws JsonProcessingException {

		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		// Kiểm tra logic ngày tháng
		if (request.getStartDate().isAfter(request.getEndDate())) {
			throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
		}

		// Kiểm tra mã khuyến mãi đã tồn tại chưa
		if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
			boolean promoCodeExists = promotionRepository
					.existsByPromoCode(request.getPromoCode().trim().toUpperCase());
			if (promoCodeExists) {
				throw new IllegalArgumentException(
						"Mã khuyến mãi '" + request.getPromoCode() + "' đã tồn tại. Vui lòng chọn mã khác.");
			}
		}

		Promotion promotion = new Promotion();
		promotion.setCreatedByUserID(user);
		promotion.setCreatedByShopID(shop);
		promotion.setPromotionName(request.getPromotionName());
		promotion.setDescription(request.getDescription());
		promotion.setPromoCode(request.getPromoCode());
		promotion.setStartDate(request.getStartDate());
		promotion.setEndDate(request.getEndDate());

		// *** FIX: Xử lý usageLimit - NULL nếu không giới hạn ***
		promotion.setUsageLimit(normalizeUsageLimit(request.getUsageLimit()));

		promotion.setPromotionType("ORDER");
		promotion.setStatus((byte) 0);

		if (request.getDiscountType() == null || request.getDiscountValue() == null) {
			throw new IllegalArgumentException("Loại giảm giá và Giá trị giảm giá là bắt buộc.");
		}
		promotion.setDiscountType(request.getDiscountType());
		promotion.setDiscountValue(request.getDiscountValue());
		promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
		promotion.setMinOrderValue(request.getMinOrderValue());

		try {
			Promotion savedPromotion = promotionRepository.save(promotion);

			// Tạo yêu cầu phê duyệt
			PromotionApproval approval = new PromotionApproval();
			approval.setPromotion(savedPromotion);
			approval.setActionType("CREATE");
			approval.setStatus("Pending");
			approval.setChangeDetails(objectMapper.writeValueAsString(request));
			approval.setRequestedBy(user);

			promotionApprovalRepository.save(approval);
			notificationService.notifyAdminsAboutNewApproval("PROMOTION", savedPromotion.getPromotionId());

		} catch (DataIntegrityViolationException e) {
			log.error("Database constraint violation", e);
			throw new IllegalArgumentException(
					"Mã khuyến mãi '" + request.getPromoCode() + "' đã tồn tại. Vui lòng chọn mã khác.");
		}
	}

	public List<SimpleProductDTO> getShopProductsForSelection(Integer shopId) {
		List<Product> products = productRepository.findActiveProductsByShop(shopId);
		return products.stream().map(p -> new SimpleProductDTO(p.getProductID(), p.getProductName()))
				.collect(Collectors.toList());
	}

	@Transactional
	public void requestPromotionUpdate(Integer shopId, PromotionRequestDTO request, Integer userId) throws Exception {
		log.info("=== START requestPromotionUpdate ===");
		log.info("Shop ID: {}, Promotion ID: {}, User ID: {}", shopId, request.getPromotionId(), userId);
		log.info("Request UsageLimit BEFORE normalize: {}", request.getUsageLimit());

		Promotion promotion = getPromotionDetail(shopId, request.getPromotionId());
		log.info("Current Promotion UsageLimit in DB: {}", promotion.getUsageLimit());

		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		if ("PRODUCT".equals(promotion.getPromotionType())) {
			log.warn("Cannot edit PRODUCT type promotion");
			throw new RuntimeException(
					"Không thể chỉnh sửa khuyến mãi sản phẩm. Vui lòng sửa ở trang Quản lý sản phẩm.");
		}

		List<PromotionApproval> existingApprovals = promotionApprovalRepository
				.findByPromotion_PromotionIdAndStatus(promotion.getPromotionId(), "Pending");
		if (!existingApprovals.isEmpty()) {
			log.warn("Pending approval already exists");
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho khuyến mãi này");
		}

		if (request.getStartDate().isAfter(request.getEndDate())) {
			log.warn("Invalid date range: start={}, end={}", request.getStartDate(), request.getEndDate());
			throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
		}

		// Kiểm tra mã khuyến mãi trùng (trừ chính nó)
		if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
			String newPromoCode = request.getPromoCode().trim().toUpperCase();
			String currentPromoCode = promotion.getPromoCode();
			log.info("Checking promo code - Current: {}, New: {}", currentPromoCode, newPromoCode);

			if (!newPromoCode.equals(currentPromoCode)) {
				boolean promoCodeExists = promotionRepository.existsByPromoCode(newPromoCode);
				if (promoCodeExists) {
					log.warn("Promo code already exists: {}", newPromoCode);
					throw new IllegalArgumentException(
							"Mã khuyến mãi '" + request.getPromoCode() + "' đã tồn tại. Vui lòng chọn mã khác.");
				}
			}
		}

		if (request.getDiscountType() == null || request.getDiscountValue() == null) {
			log.warn("Missing discount type or value");
			throw new IllegalArgumentException("Loại giảm giá và Giá trị giảm giá là bắt buộc.");
		}

		// *** FIX: Normalize usageLimit trước khi lưu vào DTO ***
		Integer originalUsageLimit = request.getUsageLimit();
		request.setUsageLimit(normalizeUsageLimit(request.getUsageLimit()));
		log.info("UsageLimit normalized: {} -> {}", originalUsageLimit, request.getUsageLimit());

		try {
			// Tạo yêu cầu phê duyệt
			String changeDetailsJson = objectMapper.writeValueAsString(request);
			log.info("ChangeDetails JSON: {}", changeDetailsJson);

			PromotionApproval approval = new PromotionApproval();
			approval.setPromotion(promotion);
			approval.setActionType("UPDATE");
			approval.setStatus("Pending");
			approval.setChangeDetails(changeDetailsJson);
			approval.setRequestedBy(user);
			approval.setRequestedAt(LocalDateTime.now());

			PromotionApproval savedApproval = promotionApprovalRepository.save(approval);
			log.info("PromotionApproval saved with ID: {}", savedApproval.getApprovalId());

			notificationService.notifyAdminsAboutNewApproval("PROMOTION", promotion.getPromotionId());
			log.info("Admin notification sent");
			log.info("=== END requestPromotionUpdate SUCCESS ===");

		} catch (DataIntegrityViolationException e) {
			log.error("Database constraint violation", e);
			throw new IllegalArgumentException(
					"Mã khuyến mãi '" + request.getPromoCode() + "' đã tồn tại. Vui lòng chọn mã khác.");
		} catch (Exception e) {
			log.error("Unexpected error in requestPromotionUpdate", e);
			throw e;
		}
	}

	public void requestPromotionDeletion(Integer shopId, Integer promotionId, Integer userId) {
		Promotion promotion = promotionRepository.findById(promotionId)
				.orElseThrow(() -> new RuntimeException("Promotion not found"));

		if (!promotion.getCreatedByShopID().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Promotion does not belong to this shop");
		}

		List<PromotionApproval> existingApprovals = promotionApprovalRepository
				.findByPromotion_PromotionIdAndStatus(promotionId, "Pending");

		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho khuyến mãi này");
		}

		PromotionApproval approval = new PromotionApproval();
		approval.setPromotion(promotion);
		approval.setActionType("DELETE");
		approval.setStatus("Pending");
		approval.setRequestedBy(
				userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
		approval.setRequestedAt(LocalDateTime.now());

		promotionApprovalRepository.save(approval);

		notificationService.notifyAdminsAboutNewApproval("PROMOTION", promotion.getPromotionId());

		log.info("Promotion deletion requested - Promotion ID: {}, Shop ID: {}", promotion.getPromotionId(), shopId);
	}

	/**
	 * *** HELPER METHOD: Chuẩn hóa usageLimit *** Chuyển 0 hoặc số âm thành NULL
	 * (không giới hạn) Giữ nguyên giá trị > 0
	 */
	private Integer normalizeUsageLimit(Integer usageLimit) {
		if (usageLimit == null || usageLimit <= 0) {
			return null; // Không giới hạn
		}
		return usageLimit;
	}
}