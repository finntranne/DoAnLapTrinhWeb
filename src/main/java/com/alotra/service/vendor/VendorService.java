package com.alotra.service.vendor;

import com.alotra.dto.*;
import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.dto.product.ProductStatisticsDTO;
import com.alotra.dto.product.ProductVariantDTO;
import com.alotra.dto.product.SimpleCategoryDTO;
import com.alotra.dto.product.SimpleSizeDTO;
import com.alotra.dto.promotion.PromotionStatisticsDTO;
import com.alotra.dto.promotion.PromotionRequestDTO;
import com.alotra.dto.response.ApprovalResponseDTO;
import com.alotra.dto.shop.ShopDashboardDTO;
import com.alotra.dto.shop.ShopOrderDTO;
import com.alotra.dto.shop.ShopRevenueDTO;
import com.alotra.entity.*;
import com.alotra.entity.order.Order;
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductApproval;
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Size;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.promotion.PromotionApproval;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.shop.ShopRevenue;
import com.alotra.entity.user.User;
import com.alotra.repository.*;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.product.CategoryRepository;
import com.alotra.repository.product.ProductApprovalRepository;
import com.alotra.repository.product.ProductImageRepository;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.product.ProductVariantRepository;
import com.alotra.repository.product.SizeRepository;
import com.alotra.repository.promotion.PromotionApprovalRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.shop.ShopRevenueRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.cloudinary.CloudinaryService;
import com.alotra.service.notification.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorService {

	private final ShopRepository shopRepository;
	private final ProductRepository productRepository;
	private final ProductApprovalRepository productApprovalRepository;
	private final PromotionRepository promotionRepository;
	private final PromotionApprovalRepository promotionApprovalRepository;
	private final OrderRepository orderRepository;
	private final ShopRevenueRepository shopRevenueRepository;
	private final ProductVariantRepository productVariantRepository;
	private final ProductImageRepository productImageRepository;
	private final SizeRepository sizeRepository;
	private final CategoryRepository categoryRepository;
	private final CloudinaryService cloudinaryService;
	private final NotificationService notificationService;
	private final UserRepository userRepository;

	private final ObjectMapper objectMapper;

	// ==================== DASHBOARD ====================

	public ShopDashboardDTO getShopDashboard(Integer shopId) {
		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));

		ShopDashboardDTO dashboard = new ShopDashboardDTO();
		dashboard.setShopId(shopId);
		dashboard.setShopName(shop.getShopName());
		dashboard.setLogoUrl(shop.getLogoURL());

		// Thống kê sản phẩm
		dashboard.setTotalProducts(productRepository.countByShopIdAndStatus(shopId, null).intValue());
		dashboard.setActiveProducts(productRepository.countByShopIdAndStatus(shopId, (byte) 1).intValue());

		// Thống kê phê duyệt đang chờ
		Long pendingProducts = productApprovalRepository.countPendingByShopId(shopId);
		Long pendingPromotions = promotionApprovalRepository.countPendingByShopId(shopId);
		dashboard.setPendingApprovals((int) (pendingProducts + pendingPromotions));

		// Thống kê đơn hàng
		dashboard.setTotalOrders(orderRepository.countByShopId(shopId));
		dashboard.setPendingOrders(orderRepository.countByShopIdAndStatus(shopId, "Pending"));
		dashboard.setDeliveringOrders(orderRepository.countByShopIdAndStatus(shopId, "Delivering"));

		// Thống kê doanh thu
		Double totalRevenue = shopRevenueRepository.getTotalRevenueByShopId(shopId);
		dashboard.setTotalRevenue(BigDecimal.valueOf(totalRevenue != null ? totalRevenue : 0));

		LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
		LocalDateTime endOfMonth = LocalDateTime.now();
		Double monthRevenue = shopRevenueRepository.getRevenueByShopIdAndDateRange(shopId, startOfMonth, endOfMonth);
		dashboard.setThisMonthRevenue(BigDecimal.valueOf(monthRevenue != null ? monthRevenue : 0));

		return dashboard;
	}

	// ==================== PRODUCT MANAGEMENT ====================

	public Page<ProductStatisticsDTO> getShopProducts(Integer shopId, Byte status, Integer categoryId, String search,
			Pageable pageable) {
		// *** PASS categoryId TO REPOSITORY ***
		Page<Product> products = productRepository.searchShopProducts(shopId, status, categoryId, search, pageable);

		// The rest of the mapping logic remains the same
		return products.map(product -> {
			ProductStatisticsDTO dto = new ProductStatisticsDTO();
			// ... (Gán các thuộc tính khác như cũ)
			dto.setProductId(product.getProductID());
			dto.setProductName(product.getProductName());
			dto.setSoldCount(product.getSoldCount());
			dto.setAverageRating(product.getAverageRating());
			dto.setTotalReviews(product.getTotalReviews());
			dto.setViewCount(product.getViewCount());
			// Use DTO status directly
			dto.setStatus(product.getStatus() == 1 ? "Đang hoạt động" : "Không hoạt động"); // Updated status text
			product.getImages().stream().filter(ProductImage::getIsPrimary).findFirst()
					.ifPresent(img -> dto.setPrimaryImageUrl(img.getImageURL()));
			product.getVariants().stream().map(ProductVariant::getPrice).min(BigDecimal::compareTo)
					.ifPresent(dto::setMinPrice);

			Optional<ProductApproval> latestApprovalOpt = productApprovalRepository
					.findTopByProduct_ProductIDOrderByRequestedAtDesc(product.getProductID());

			if (latestApprovalOpt.isPresent()) {
				ProductApproval latestApproval = latestApprovalOpt.get();
				String currentStatus = latestApproval.getStatus();

				if ("Pending".equals(currentStatus) || "Rejected".equals(currentStatus)) {
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

					if ("Pending".equals(currentStatus)) {
						dto.setApprovalStatus("Đang chờ: " + actionTypeText);
					} else {
						dto.setApprovalStatus("Bị từ chối: " + actionTypeText);
					}
				}
			}
			return dto;
		});
	}

	public Product getProductDetail(Integer shopId, Integer productId) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new RuntimeException("Product not found"));

		if (!product.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Product does not belong to this shop");
		}

		return product;
	}

	public void requestProductCreation(Integer shopId, ProductRequestDTO request, Integer userId)
			throws JsonProcessingException { // Thêm throws Exception nếu uploadImageAndReturnDetails có thể ném lỗi I/O

		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));

		// Tạo sản phẩm mới với status = 0 (Inactive)
		Product product = new Product();
		product.setShop(shop);
		product.setCategory(categoryRepository.findById(request.getCategoryId())
				.orElseThrow(() -> new RuntimeException("Category not found")));
		product.setProductName(request.getProductName());
		product.setDescription(request.getDescription());
		product.setStatus((byte) 0); // Inactive until approved
		product.setCreatedAt(LocalDateTime.now());
		product.setUpdatedAt(LocalDateTime.now());

		product = productRepository.save(product);

		log.info("Product entity created with ID: {}", product.getProductID());

		// *** START SỬA ĐỔI PHẦN UPLOAD ẢNH ***
		if (request.getImages() != null && !request.getImages().isEmpty()) {
			boolean hasValidImage = request.getImages().stream().anyMatch(file -> file != null && !file.isEmpty());

			if (hasValidImage) {
				log.info("Processing images for new product ID: {}", product.getProductID());
				for (int i = 0; i < request.getImages().size(); i++) {
					MultipartFile file = request.getImages().get(i);
					if (file != null && !file.isEmpty()) {
						try {
							// *** THAY ĐỔI LỜI GỌI: Sử dụng uploadImageAndReturnDetails và chỉ định folder
							// "products" ***
							Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(file,
									"products", userId);
							String imageUrl = uploadResult.get("secure_url");
							// String publicId = uploadResult.get("public_id"); // Lấy publicId nếu cần lưu

							if (imageUrl == null) {
								log.error("Cloudinary upload did not return URL for image at index {}", i);
								throw new RuntimeException("Lỗi khi upload hình ảnh: Không nhận được URL.");
							}
							// *** KẾT THÚC THAY ĐỔI LỜI GỌI ***

							ProductImage productImage = new ProductImage();
							productImage.setProduct(product);
							productImage.setImageURL(imageUrl);
							productImage.setIsPrimary(
									i == (request.getPrimaryImageIndex() != null ? request.getPrimaryImageIndex() : 0));
							productImage.setDisplayOrder(i);
							productImageRepository.save(productImage);

							log.info("Image uploaded and saved: {}", imageUrl);

						} catch (Exception e) { // Bắt Exception chung (bao gồm IOException và RuntimeException)
							log.error("Error uploading image at index {}: {}", i, e.getMessage(), e);
							// Ném lại lỗi để transaction rollback
							throw new RuntimeException("Lỗi khi upload hình ảnh: " + e.getMessage());
						}
					}
				}
			} else {
				log.warn("Image list provided but contains no valid files for product ID: {}", product.getProductID());
				// Cân nhắc: Có nên throw lỗi nếu ảnh là bắt buộc không? Dựa vào logic trước đó
				// thì có.
				throw new RuntimeException("Vui lòng upload ít nhất một hình ảnh hợp lệ.");
			}
		} else {
			// Logic validation ở Controller đã kiểm tra, nhưng thêm log ở đây để chắc chắn
			log.warn("No images provided for new product request.");
			throw new RuntimeException("Vui lòng upload ít nhất một hình ảnh.");
		}
		// *** END SỬA ĐỔI PHẦN UPLOAD ẢNH ***

		// Tạo variants (Giữ nguyên logic cũ)
		if (request.getVariants() != null && !request.getVariants().isEmpty()) {
			for (ProductVariantDTO variantDTO : request.getVariants()) {
				ProductVariant variant = new ProductVariant();
				variant.setProduct(product);
				variant.setSize(sizeRepository.findById(variantDTO.getSizeId())
						.orElseThrow(() -> new RuntimeException("Size not found")));
				variant.setPrice(variantDTO.getPrice());
				variant.setStock(variantDTO.getStock());
				variant.setSku(variantDTO.getSku());
				productVariantRepository.save(variant);

				log.info("Variant saved: Size={}, Price={}, Stock={}", variantDTO.getSizeId(), variantDTO.getPrice(),
						variantDTO.getStock());
			}
		} else {
			throw new RuntimeException("Sản phẩm phải có ít nhất một biến thể");
		}

		// Tạo yêu cầu phê duyệt (Giữ nguyên logic cũ)
		ProductApproval approval = new ProductApproval();
		approval.setProduct(product);
		approval.setActionType("CREATE");
		approval.setStatus("Pending");
		// Chuyển DTO thành JSON (DTO này không chứa newImageUrls vì không cần cho
		// CREATE)
		approval.setChangeDetails(objectMapper.writeValueAsString(request));
		approval.setRequestedBy(
				userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
		approval.setRequestedAt(LocalDateTime.now());

		approval = productApprovalRepository.save(approval);

		log.info("Product approval created with ID: {}", approval.getApprovalId());

		// Gửi thông báo cho admin (Giữ nguyên logic cũ)
		try {
			notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
		} catch (Exception e) {
			log.error("Error sending notification: {}", e.getMessage());
		}

		log.info("Product creation requested - Product ID: {}, Shop ID: {}, Approval ID: {}", product.getProductID(),
				shopId, approval.getApprovalId());
	}

	public void requestProductUpdate(Integer shopId, ProductRequestDTO request, Integer userId)
			throws JsonProcessingException { // Add potential Exception from upload

		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new RuntimeException("Product not found"));

		if (!product.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Product does not belong to this shop");
		}

		List<ProductApproval> existingApprovals = productApprovalRepository
				.findByProduct_ProductIDAndStatus(product.getProductID(), "Pending");

		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho sản phẩm này");
		}

		// *** START IMAGE UPDATE HANDLING ***
		List<String> uploadedImageUrls = new ArrayList<>();
		// List<String> uploadedImagePublicIds = new ArrayList<>(); // Optional

		// Check if new images were actually submitted
		boolean newImagesSubmitted = request.getImages() != null && !request.getImages().isEmpty()
				&& request.getImages().stream().anyMatch(f -> f != null && !f.isEmpty());

		if (newImagesSubmitted) {
			log.info("Processing new images for product update ID: {}", request.getProductId());
			for (MultipartFile file : request.getImages()) {
				if (file != null && !file.isEmpty()) {
					try {
						// Assuming uploadImage returns Map<String, String>
						// Adjust if your service returns something else (e.g., just the URL)
						Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(file,
								"products", userId); // Use a
						// method
						// returning
						// details
						String imageUrl = uploadResult.get("secure_url");
						// String publicId = uploadResult.get("public_id"); // Optional

						if (imageUrl != null) {
							uploadedImageUrls.add(imageUrl);
							// uploadedImagePublicIds.add(publicId); // Optional
							log.info("Uploaded new image: {}", imageUrl);
						} else {
							log.warn("Cloudinary upload did not return a URL for one of the files.");
						}

					} catch (Exception e) {
						log.error("Error uploading a new image during product update: {}", e.getMessage(), e);
						// Decide: Throw exception to stop, or just log and continue without the failed
						// image?
						// Throwing is safer to ensure consistency.
						throw new RuntimeException("Lỗi khi upload hình ảnh mới: " + e.getMessage());
					}
				} else {
					// Handle potential null/empty entries if the list allows them
					// Or ensure the list passed from the controller is clean
				}
			}
			// Populate the DTO fields ONLY IF new images were processed
			if (!uploadedImageUrls.isEmpty()) {
				request.setNewImageUrls(uploadedImageUrls);
				// request.setNewImagePublicIds(uploadedImagePublicIds); // Optional
			} else {
				// If upload resulted in no URLs (e.g., all failed, or empty files submitted)
				// Ensure the fields are null or empty list so JSON doesn't contain them
				// accidentally
				request.setNewImageUrls(null);
				// request.setNewImagePublicIds(null);
				log.warn("New images were submitted, but none were successfully uploaded or returned URLs.");
			}
		} else {
			// No new image files submitted, ensure fields are null/empty
			request.setNewImageUrls(null);
			// request.setNewImagePublicIds(null);
			log.info("No new image files submitted for product update ID: {}", request.getProductId());
		}
		// *** END IMAGE UPDATE HANDLING ***

		// Create approval request - NOW the DTO contains new image info (if any)
		ProductApproval approval = new ProductApproval();
		approval.setProduct(product);
		approval.setActionType("UPDATE");
		approval.setStatus("Pending");
		approval.setChangeDetails(objectMapper.writeValueAsString(request)); // Serialize the UPDATED DTO
		approval.setRequestedBy(
				userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
		approval.setRequestedAt(LocalDateTime.now());

		approval = productApprovalRepository.save(approval);

		log.info("Product approval created with ID: {}", approval.getApprovalId());

		try {
			notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
		} catch (Exception e) {
			log.error("Error sending notification: {}", e.getMessage());
		}

		log.info("Product update requested - Product ID: {}, Shop ID: {}, Approval ID: {}", product.getProductID(),
				shopId, approval.getApprovalId());
	}

	public void requestProductDeletion(Integer shopId, Integer productId, Integer userId) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new RuntimeException("Product not found"));

		if (!product.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Product does not belong to this shop");
		}

		// Kiểm tra pending requests
		List<ProductApproval> existingApprovals = productApprovalRepository
				.findByProduct_ProductIDAndStatus(product.getProductID(), "Pending");

		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho sản phẩm này");
		}

		// Tạo yêu cầu phê duyệt
		ProductApproval approval = new ProductApproval();
		approval.setProduct(product);
		approval.setActionType("DELETE");
		approval.setStatus("Pending");
		approval.setRequestedBy(
				userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
		approval.setRequestedAt(LocalDateTime.now());

		approval = productApprovalRepository.save(approval);

		log.info("Product approval created with ID: {}", approval.getApprovalId());

		// Gửi thông báo
		try {
			notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
		} catch (Exception e) {
			log.error("Error sending notification: {}", e.getMessage());
		}

		log.info("Product deletion requested - Product ID: {}, Shop ID: {}, Approval ID: {}", product.getProductID(),
				shopId, approval.getApprovalId());
	}

	public List<SimpleCategoryDTO> getAllCategoriesSimple() {
		return categoryRepository.findAll().stream()
				.map(c -> new SimpleCategoryDTO(c.getCategoryID(), c.getCategoryName())).collect(Collectors.toList());
	}

	public List<SimpleSizeDTO> getAllSizesSimple() {
		return sizeRepository.findAll().stream().map(s -> new SimpleSizeDTO(s.getSizeID(), s.getSizeName()))
				.collect(Collectors.toList());
	}

	public List<Category> getAllCategories() {
		return categoryRepository.findAll();
	}

	public List<Size> getAllSizes() {
		return sizeRepository.findAll();
	}

	public ProductRequestDTO convertProductToDTO(Product product) {
		ProductRequestDTO dto = new ProductRequestDTO();

		dto.setProductId(product.getProductID());
		dto.setProductName(product.getProductName());
		dto.setDescription(product.getDescription());
		dto.setCategoryId(product.getCategory().getCategoryID());

		// Convert variants
		List<ProductVariantDTO> variantDTOs = new ArrayList<>();
		for (ProductVariant variant : product.getVariants()) {
			ProductVariantDTO variantDTO = new ProductVariantDTO();
			variantDTO.setVariantId(variant.getVariantID());
			variantDTO.setSizeId(variant.getSize().getSizeID());
			variantDTO.setPrice(variant.getPrice());
			variantDTO.setStock(variant.getStock());
			variantDTO.setSku(variant.getSku());
			variantDTOs.add(variantDTO);
		}
		dto.setVariants(variantDTOs);

		// Find primary image index
		List<ProductImage> images = product.getImages().stream()
				.sorted(Comparator.comparing(ProductImage::getDisplayOrder)).collect(Collectors.toList());

		for (int i = 0; i < images.size(); i++) {
			if (images.get(i).getIsPrimary()) {
				dto.setPrimaryImageIndex(i);
				break;
			}
		}

		return dto;
	}

	// ==================== PROMOTION MANAGEMENT ====================

	public Page<PromotionStatisticsDTO> getShopPromotions(Integer shopId, Byte status, Pageable pageable) {

		// *** ĐÃ SỬA: Gọi phương thức repository mới và truyền LocalDateTime.now() ***
		LocalDateTime now = LocalDateTime.now();
		Page<Promotion> promotions = promotionRepository.findShopPromotionsFiltered(shopId, status, now, pageable);
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

	public void requestPromotionCreation(Integer shopId, PromotionRequestDTO request, Integer userId)
			throws JsonProcessingException {

		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));

		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		// Tạo promotion mới với status = 0 (Inactive)
		Promotion promotion = new Promotion();
		promotion.setCreatedByUserID(user);
		promotion.setCreatedByShopID(shop);
		promotion.setPromotionName(request.getPromotionName());
		promotion.setDescription(request.getDescription());
		promotion.setPromoCode(request.getPromoCode());
		promotion.setDiscountType(request.getDiscountType());
		promotion.setDiscountValue(request.getDiscountValue());
		promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
		promotion.setStartDate(request.getStartDate());
		promotion.setEndDate(request.getEndDate());
		promotion.setMinOrderValue(request.getMinOrderValue());
		promotion.setUsageLimit(request.getUsageLimit());
		promotion.setUsedCount(0);
		promotion.setStatus((byte) 0); // Inactive until approved
		promotion.setCreatedAt(LocalDateTime.now());

		promotion = promotionRepository.save(promotion);

		// Tạo yêu cầu phê duyệt
		PromotionApproval approval = new PromotionApproval();
		approval.setPromotion(promotion);
		approval.setActionType("CREATE");
		approval.setStatus("Pending");
		approval.setChangeDetails(objectMapper.writeValueAsString(request));
		approval.setRequestedBy(user);
		approval.setRequestedAt(LocalDateTime.now());

		promotionApprovalRepository.save(approval);

		notificationService.notifyAdminsAboutNewApproval("PROMOTION", promotion.getPromotionId());

		log.info("Promotion creation requested - Promotion ID: {}, Shop ID: {}", promotion.getPromotionId(), shopId);
	}

	public void requestPromotionUpdate(Integer shopId, PromotionRequestDTO request, Integer userId)
			throws JsonProcessingException {

		Promotion promotion = promotionRepository.findById(request.getPromotionId())
				.orElseThrow(() -> new RuntimeException("Promotion not found"));

		if (!promotion.getCreatedByShopID().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Promotion does not belong to this shop");
		}

		// *** ĐÃ SỬA: Kiểm tra bất kỳ yêu cầu pending nào (giống Product) ***
		List<PromotionApproval> existingApprovals = promotionApprovalRepository
				.findByPromotion_PromotionIdAndStatus(promotion.getPromotionId(), "Pending");

		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho khuyến mãi này");
		}

		// Tạo yêu cầu phê duyệt
		PromotionApproval approval = new PromotionApproval();
		approval.setPromotion(promotion);
		approval.setActionType("UPDATE");
		approval.setStatus("Pending");
		approval.setChangeDetails(objectMapper.writeValueAsString(request));
		approval.setRequestedBy(
				userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
		approval.setRequestedAt(LocalDateTime.now());

		promotionApprovalRepository.save(approval);

		notificationService.notifyAdminsAboutNewApproval("PROMOTION", promotion.getPromotionId());

		log.info("Promotion update requested - Promotion ID: {}, Shop ID: {}", promotion.getPromotionId(), shopId);
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

	// ==================== ORDER MANAGEMENT ====================
	public Page<ShopOrderDTO> getShopOrders(Integer shopId, String status, Pageable pageable) {
		Page<Order> orders = orderRepository.findShopOrdersByStatus(shopId, status, pageable);

		return orders.map(order -> {
			ShopOrderDTO dto = new ShopOrderDTO();
			dto.setOrderId(order.getOrderID());
			dto.setOrderDate(order.getOrderDate());
			dto.setOrderStatus(order.getOrderStatus());
			dto.setPaymentMethod(order.getPaymentMethod());
			dto.setPaymentStatus(order.getPaymentStatus());
			dto.setGrandTotal(order.getGrandTotal());
			dto.setCustomerName(order.getUser().getFullName());
			dto.setCustomerPhone(order.getUser().getPhoneNumber());
			dto.setRecipientName(order.getRecipientName());
			dto.setRecipientPhone(order.getRecipientPhone());
			dto.setShippingAddress(order.getShippingAddress());

			if (order.getShipper() != null) {
				dto.setShipperName(order.getShipper().getFullName());
			}

			dto.setTotalItems(order.getOrderDetails().size());

			return dto;
		});
	}

	public Order getOrderDetail(Integer shopId, Integer orderId) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

		if (!order.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Order does not belong to this shop");
		}

		return order;
	}

	public void updateOrderStatus(Integer shopId, Integer orderId, String newStatus, Integer userId) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

		if (!order.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Order does not belong to this shop");
		}

		// Validate status transition
		validateOrderStatusTransition(order.getOrderStatus(), newStatus);

		String oldStatus = order.getOrderStatus();
		order.setOrderStatus(newStatus);

		if ("Completed".equals(newStatus)) {
			order.setCompletedAt(LocalDateTime.now());
		}

		orderRepository.save(order);

		// Gửi thông báo cho khách hàng
		notificationService.notifyCustomerAboutOrderStatus(order.getUser().getId(), orderId, newStatus);

		log.info("Order status updated - Order ID: {}, Old Status: {}, New Status: {}", orderId, oldStatus, newStatus);
	}

	private void validateOrderStatusTransition(String currentStatus, String newStatus) {
		Map<String, List<String>> allowedTransitions = new HashMap<>();
		allowedTransitions.put("Pending", Arrays.asList("Confirmed", "Cancelled"));
		allowedTransitions.put("Confirmed", Arrays.asList("Delivering", "Cancelled"));
		allowedTransitions.put("Delivering", Arrays.asList("Completed", "Returned"));

		List<String> allowed = allowedTransitions.get(currentStatus);
		if (allowed == null || !allowed.contains(newStatus)) {
			throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + newStatus);
		}
	}

	// ==================== REVENUE MANAGEMENT ====================

	public List<ShopRevenueDTO> getShopRevenue(Integer shopId, LocalDateTime startDate, LocalDateTime endDate) {
		if (startDate == null) {
			startDate = LocalDateTime.now().minusMonths(1);
		}
		if (endDate == null) {
			endDate = LocalDateTime.now();
		}

		List<ShopRevenue> revenues = shopRevenueRepository.findByShopIdAndDateRange(shopId, startDate, endDate);

		// Group by date
		Map<LocalDateTime, List<ShopRevenue>> groupedByDate = revenues.stream()
				.collect(Collectors.groupingBy(sr -> sr.getRecordedAt().toLocalDate().atStartOfDay()));

		return groupedByDate.entrySet().stream().map(entry -> {
			ShopRevenueDTO dto = new ShopRevenueDTO();
			dto.setDate(entry.getKey());
			dto.setTotalOrders((long) entry.getValue().size());
			dto.setOrderAmount(entry.getValue().stream().map(ShopRevenue::getOrderAmount).reduce(BigDecimal.ZERO,
					BigDecimal::add));
			dto.setCommissionAmount(entry.getValue().stream().map(ShopRevenue::getCommissionAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add));
			dto.setNetRevenue(
					entry.getValue().stream().map(ShopRevenue::getNetRevenue).reduce(BigDecimal.ZERO, BigDecimal::add));
			return dto;
		}).sorted(Comparator.comparing(ShopRevenueDTO::getDate).reversed()).collect(Collectors.toList());
	}

	// ==================== APPROVAL STATUS ====================

	public List<ApprovalResponseDTO> getPendingApprovals(Integer shopId) {
		List<ApprovalResponseDTO> approvals = new ArrayList<>();

		// Product approvals
		List<ProductApproval> productApprovals = productApprovalRepository
				.findByProduct_Shop_ShopIdAndStatusOrderByRequestedAtDesc(shopId, "Pending");

		for (ProductApproval pa : productApprovals) {
			ApprovalResponseDTO dto = new ApprovalResponseDTO();
			dto.setApprovalId(pa.getApprovalId());
			dto.setEntityType("PRODUCT");
			dto.setEntityId(pa.getProduct().getProductID());
			dto.setActionType(pa.getActionType());
			dto.setStatus(pa.getStatus());
			dto.setChangeDetails(pa.getChangeDetails());
			dto.setRequestedAt(pa.getRequestedAt());
			dto.setRequestedByName(pa.getRequestedBy().getFullName());
			approvals.add(dto);
		}

		// Promotion approvals
		List<PromotionApproval> promotionApprovals = promotionApprovalRepository
				.findByPromotion_CreatedByShopID_ShopIdAndStatusOrderByRequestedAtDesc(shopId, "Pending");

		for (PromotionApproval pa : promotionApprovals) {
			ApprovalResponseDTO dto = new ApprovalResponseDTO();
			dto.setApprovalId(pa.getApprovalId());
			dto.setEntityType("PROMOTION");
			dto.setEntityId(pa.getPromotion().getPromotionId());
			dto.setActionType(pa.getActionType());
			dto.setStatus(pa.getStatus());
			dto.setChangeDetails(pa.getChangeDetails());
			dto.setRequestedAt(pa.getRequestedAt());
			dto.setRequestedByName(pa.getRequestedBy().getFullName());
			approvals.add(dto);
		}

		// Sort by requested date
		approvals.sort(Comparator.comparing(ApprovalResponseDTO::getRequestedAt).reversed());

		return approvals;
	}
}