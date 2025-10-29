package com.alotra.service.vendor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
	@PersistenceContext // Inject EntityManager for JPQL
	private EntityManager entityManager;

	public Page<PromotionStatisticsDTO> getShopPromotions(Integer shopId, Byte status, String promotionType,
			Pageable pageable) {

		// *** ĐÃ SỬA: Gọi phương thức repository mới và truyền LocalDateTime.now() ***
		LocalDateTime now = LocalDateTime.now();
		Page<Promotion> promotions = promotionRepository.findShopPromotionsFiltered(shopId, status, promotionType, now,
				pageable);
		// *** KẾT THÚC SỬA ***

		// Phần map sang PromotionListDTO giữ nguyên như trước
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

			// Tính toán Trạng thái Hoạt động (Dựa vào Status và EndDate)
			// Logic này vẫn đúng vì nó tính toán sau khi đã lọc từ DB
			if (promotion.getStatus() == 0) {
				activityStatus = "Không hoạt động";
			} else if (promotion.getEndDate().isBefore(now)) { // So sánh với 'now'
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
		// 1. Chuyển đổi các trường cơ bản
		PromotionRequestDTO dto = convertPromotionToDTO(promotion);

		// 2. Nếu là loại "PRODUCT", tải các sản phẩm liên kết
		if ("PRODUCT".equals(promotion.getPromotionType())) {
			// Lấy danh sách PromotionProduct từ DB
			List<PromotionProduct> promoProducts = promotionProductRepository.findByPromotion(promotion);

			// Chuyển đổi sang List<ProductDiscountDTO>
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

		Promotion promotion = new Promotion();
		promotion.setCreatedByUserID(user);
		promotion.setCreatedByShopID(shop);
		promotion.setPromotionName(request.getPromotionName());
		promotion.setDescription(request.getDescription());
		promotion.setPromoCode(request.getPromoCode());
		promotion.setStartDate(request.getStartDate());
		promotion.setEndDate(request.getEndDate());
		promotion.setUsageLimit(request.getUsageLimit());
		promotion.setPromotionType("ORDER"); // *** CHỈ CÒN ORDER ***
		promotion.setStatus((byte) 0); // Pending

		// *** CHỈ XỬ LÝ LOGIC ORDER ***
		if (request.getDiscountType() == null || request.getDiscountValue() == null) {
			throw new IllegalArgumentException("Loại giảm giá và Giá trị giảm giá là bắt buộc.");
		}
		promotion.setDiscountType(request.getDiscountType());
		promotion.setDiscountValue(request.getDiscountValue());
		promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
		promotion.setMinOrderValue(request.getMinOrderValue());

		Promotion savedPromotion = promotionRepository.save(promotion);

		// *** BỎ LOGIC XỬ LÝ productDiscounts ***

		// Tạo yêu cầu phê duyệt
		PromotionApproval approval = new PromotionApproval();
		approval.setPromotion(savedPromotion);
		approval.setActionType("CREATE");
		approval.setStatus("Pending");
		approval.setChangeDetails(objectMapper.writeValueAsString(request));
		approval.setRequestedBy(user);

		promotionApprovalRepository.save(approval);
		notificationService.notifyAdminsAboutNewApproval("PROMOTION", savedPromotion.getPromotionId());
	}

	public List<SimpleProductDTO> getShopProductsForSelection(Integer shopId) {
		List<Product> products = productRepository.findActiveProductsByShop(shopId);
		return products.stream().map(p -> new SimpleProductDTO(p.getProductID(), p.getProductName()))
				.collect(Collectors.toList());
	}

	@Transactional
	public void requestPromotionUpdate(Integer shopId, PromotionRequestDTO request, Integer userId) throws Exception {

		Promotion promotion = getPromotionDetail(shopId, request.getPromotionId());
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		// *** KIỂM TRA: KHÔNG CHO UPDATE PROMOTION PRODUCT TYPE ***
		if ("PRODUCT".equals(promotion.getPromotionType())) {
			throw new RuntimeException(
					"Không thể chỉnh sửa khuyến mãi sản phẩm. Vui lòng sửa ở trang Quản lý sản phẩm.");
		}

		List<PromotionApproval> existingApprovals = promotionApprovalRepository
				.findByPromotion_PromotionIdAndStatus(promotion.getPromotionId(), "Pending");
		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho khuyến mãi này");
		}

		if (request.getStartDate().isAfter(request.getEndDate())) {
			throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
		}

		// *** CHỈ VALIDATE CHO ORDER TYPE ***
		if (request.getDiscountType() == null || request.getDiscountValue() == null) {
			throw new IllegalArgumentException("Loại giảm giá và Giá trị giảm giá là bắt buộc.");
		}

		// *** BỎ LOGIC productDiscounts ***

		// Tạo yêu cầu phê duyệt
		PromotionApproval approval = new PromotionApproval();
		approval.setPromotion(promotion);
		approval.setActionType("UPDATE");
		approval.setStatus("Pending");
		approval.setChangeDetails(objectMapper.writeValueAsString(request));
		approval.setRequestedBy(user);
		approval.setRequestedAt(LocalDateTime.now());

		promotionApprovalRepository.save(approval);
		notificationService.notifyAdminsAboutNewApproval("PROMOTION", promotion.getPromotionId());
	}

	public void requestPromotionDeletion(Integer shopId, Integer promotionId, Integer userId) {
		Promotion promotion = promotionRepository.findById(promotionId)
				.orElseThrow(() -> new RuntimeException("Promotion not found"));

		if (!promotion.getCreatedByShopID().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Promotion does not belong to this shop");
		}

		// *** ĐÃ THÊM: Kiểm tra pending requests (giống Product) ***
		List<PromotionApproval> existingApprovals = promotionApprovalRepository
				.findByPromotion_PromotionIdAndStatus(promotionId, "Pending");

		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho khuyến mãi này");
		}

		// Tạo yêu cầu phê duyệt
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

}
