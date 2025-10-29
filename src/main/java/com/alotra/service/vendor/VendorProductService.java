package com.alotra.service.vendor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.dto.product.ProductStatisticsDTO;
import com.alotra.dto.product.ProductVariantDTO;
import com.alotra.dto.product.SimpleCategoryDTO;
import com.alotra.dto.product.SimpleSizeDTO;
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductApproval;
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Size;
import com.alotra.entity.product.Topping;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.promotion.PromotionProduct;
import com.alotra.entity.promotion.PromotionProductId;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.repository.product.CategoryRepository;
import com.alotra.repository.product.ProductApprovalRepository;
import com.alotra.repository.product.ProductImageRepository;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.product.ProductVariantRepository;
import com.alotra.repository.product.SizeRepository;
import com.alotra.repository.promotion.PromotionProductRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.cloudinary.CloudinaryService;
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
public class VendorProductService {
	private final ShopRepository shopRepository;
	private final ProductRepository productRepository;
	private final ProductApprovalRepository productApprovalRepository;
	private final ProductVariantRepository productVariantRepository;
	private final ProductImageRepository productImageRepository;
	private final PromotionRepository promotionRepository;
	private final PromotionProductRepository promotionProductRepository;
	private final SizeRepository sizeRepository;
	private final CategoryRepository categoryRepository;
	private final CloudinaryService cloudinaryService;
	private final NotificationService notificationService;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;
	@PersistenceContext // Inject EntityManager for JPQL
	private EntityManager entityManager;
	
	private void createInternalProductPromotion(Product product, Integer discountPercentage, Shop shop,
			Integer userId) {
		try {
			User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

			// Tạo Promotion với PromotionType = "PRODUCT"
			Promotion promotion = new Promotion();
			promotion.setCreatedByUserID(user);
			promotion.setCreatedByShopID(shop);
			promotion.setPromotionName("Giảm giá sản phẩm: " + product.getProductName());
			promotion.setDescription("Khuyến mãi nội bộ cho sản phẩm");
			promotion.setPromoCode("PRODUCT_" + product.getProductID() + "_" + System.currentTimeMillis());
			promotion.setPromotionType("PRODUCT"); // *** QUAN TRỌNG ***
			promotion.setDiscountType(null); // Không dùng cho PRODUCT type
			promotion.setDiscountValue(null);
			promotion.setStartDate(LocalDateTime.now());
			promotion.setEndDate(LocalDateTime.now().plusYears(10)); // Vô thời hạn
			promotion.setMinOrderValue(BigDecimal.ZERO);
			promotion.setUsageLimit(0); // Không giới hạn
			promotion.setStatus((byte) 1); // Active ngay
			promotion.setCreatedAt(LocalDateTime.now());

			promotion = promotionRepository.save(promotion);

			// Tạo liên kết trong PromotionProduct
			PromotionProduct pp = new PromotionProduct();
			pp.setId(new PromotionProductId(promotion.getPromotionId(), product.getProductID()));
			pp.setPromotion(promotion);
			pp.setProduct(product);
			pp.setDiscountPercentage(discountPercentage);

			promotionProductRepository.save(pp);

			log.info("Created internal product promotion ID: {} for product ID: {} with {}% discount",
					promotion.getPromotionId(), product.getProductID(), discountPercentage);

		} catch (Exception e) {
			log.error("Error creating internal product promotion: {}", e.getMessage(), e);
			// Không throw exception để không làm fail toàn bộ flow
		}
	}
	
	// ==================== PRODUCT MANAGEMENT ====================

		public Page<ProductStatisticsDTO> getShopProducts(Integer shopId, Byte status, Integer categoryId,
				String approvalStatus, String search, Pageable pageable) {

			String normalizedSearch = (search != null && search.trim().isEmpty()) ? null : search;
			String normalizedApprovalStatus = (approvalStatus != null && approvalStatus.trim().isEmpty()) ? null
					: approvalStatus;

			log.debug("Calling repository with: shopId={}, status={}, categoryId={}, approvalStatus='{}', search='{}'",
					shopId, status, categoryId, normalizedApprovalStatus, normalizedSearch);

			Page<Product> products = productRepository.searchShopProducts(shopId, status, categoryId,
					normalizedApprovalStatus, normalizedSearch, pageable);

			return products.map(product -> {
				ProductStatisticsDTO dto = new ProductStatisticsDTO();
				dto.setProductId(product.getProductID());
				dto.setProductName(product.getProductName());
				dto.setSoldCount(product.getSoldCount());
				dto.setAverageRating(product.getAverageRating());
				dto.setTotalReviews(product.getTotalReviews());
				dto.setViewCount(product.getViewCount());
				dto.setStatus(product.getStatus() == 1 ? "Đang hoạt động" : "Không hoạt động");

				// Primary image
				product.getImages().stream().filter(img -> img != null && Boolean.TRUE.equals(img.getIsPrimary()))
						.findFirst().ifPresent(img -> dto.setPrimaryImageUrl(img.getImageURL()));

				// Min price
				product.getVariants().stream().filter(Objects::nonNull).map(ProductVariant::getPrice)
						.filter(Objects::nonNull).min(BigDecimal::compareTo).ifPresent(dto::setMinPrice);

				// *** THÊM: TÌM DISCOUNT PERCENTAGE ***
				// Tìm promotion nội bộ của product (PromotionType = "PRODUCT")
				if (product.getPromotionProducts() != null && !product.getPromotionProducts().isEmpty()) {
					product.getPromotionProducts().stream().filter(pp -> pp.getPromotion() != null)
							.filter(pp -> "PRODUCT".equals(pp.getPromotion().getPromotionType()))
							.filter(pp -> pp.getPromotion().getStatus() == 1) // Chỉ lấy promotion đang active
							.filter(pp -> {
								// Kiểm tra còn hiệu lực
								LocalDateTime now = LocalDateTime.now();
								return pp.getPromotion().getStartDate().isBefore(now)
										&& pp.getPromotion().getEndDate().isAfter(now);
							}).findFirst().ifPresent(pp -> dto.setDiscountPercentage(pp.getDiscountPercentage()));
				}

				// Approval status
				Optional<ProductApproval> latestApprovalOpt = productApprovalRepository
						.findTopByProduct_ProductIDOrderByRequestedAtDesc(product.getProductID());

				if (latestApprovalOpt.isPresent()) {
					ProductApproval latestApproval = latestApprovalOpt.get();
					String currentApprovalDbStatus = latestApproval.getStatus();

					if ("Pending".equals(currentApprovalDbStatus) || "Rejected".equals(currentApprovalDbStatus)) {
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

						if ("Pending".equals(currentApprovalDbStatus)) {
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

		@Transactional
		public void requestProductCreation(Integer shopId, ProductRequestDTO request, Integer userId,
				Set<Topping> selectedToppings) throws JsonProcessingException {

			Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));

			// Tạo sản phẩm mới với status = 0 (Inactive)
			Product product = new Product();
			product.setShop(shop);
			product.setCategory(categoryRepository.findById(request.getCategoryId())
					.orElseThrow(() -> new RuntimeException("Category not found")));
			product.setProductName(request.getProductName());
			product.setDescription(request.getDescription());
			product.setStatus((byte) 0);
			product.setCreatedAt(LocalDateTime.now());
			product.setUpdatedAt(LocalDateTime.now());
			product.setAvailableToppings(selectedToppings);

			// *** LƯU SẢN PHẨM TRƯỚC (để có ID) ***
			product = productRepository.save(product);
			log.info("Product entity created with ID: {}", product.getProductID());

			// Upload ảnh (giữ nguyên logic cũ)
			if (request.getImages() != null && !request.getImages().isEmpty()) {
				boolean hasValidImage = request.getImages().stream().anyMatch(file -> file != null && !file.isEmpty());
				if (hasValidImage) {
					log.info("Processing images for new product ID: {}", product.getProductID());
					for (int i = 0; i < request.getImages().size(); i++) {
						MultipartFile file = request.getImages().get(i);
						if (file != null && !file.isEmpty()) {
							try {
								Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(file,
										"products", userId);
								String imageUrl = uploadResult.get("secure_url");
								if (imageUrl == null) {
									throw new RuntimeException("Lỗi khi upload hình ảnh: Không nhận được URL.");
								}
								ProductImage productImage = new ProductImage();
								productImage.setProduct(product);
								productImage.setImageURL(imageUrl);
								productImage.setIsPrimary(
										i == (request.getPrimaryImageIndex() != null ? request.getPrimaryImageIndex() : 0));
								productImage.setDisplayOrder(i);
								productImageRepository.save(productImage);
								log.info("Image uploaded and saved: {}", imageUrl);
							} catch (Exception e) {
								log.error("Error uploading image at index {}: {}", i, e.getMessage(), e);
								throw new RuntimeException("Lỗi khi upload hình ảnh: " + e.getMessage());
							}
						}
					}
				} else {
					throw new RuntimeException("Vui lòng upload ít nhất một hình ảnh hợp lệ.");
				}
			} else {
				throw new RuntimeException("Vui lòng upload ít nhất một hình ảnh.");
			}

			// *** TẠO VARIANTS VÀ TỰ ĐỘNG TÍNH basePrice ***
			if (request.getVariants() != null && !request.getVariants().isEmpty()) {
				List<ProductVariant> createdVariants = new ArrayList<>();

				for (ProductVariantDTO variantDTO : request.getVariants()) {
					ProductVariant variant = new ProductVariant();
					variant.setProduct(product);
					variant.setSize(sizeRepository.findById(variantDTO.getSizeId())
							.orElseThrow(() -> new RuntimeException("Size not found")));
					variant.setPrice(variantDTO.getPrice());
					variant.setStock(variantDTO.getStock());
					variant.setSku(variantDTO.getSku());

					variant = productVariantRepository.save(variant);
					createdVariants.add(variant);

					log.info("Variant saved: Size={}, Price={}, Stock={}", variantDTO.getSizeId(), variantDTO.getPrice(),
							variantDTO.getStock());
				}

				// *** TÍNH VÀ LƯU basePrice ***
				BigDecimal minPrice = createdVariants.stream().map(ProductVariant::getPrice).filter(Objects::nonNull)
						.min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

				product.setBasePrice(minPrice);
				product = productRepository.save(product); // Lưu lại để cập nhật basePrice

				log.info("Product basePrice calculated and saved: {}", minPrice);

			} else {
				throw new RuntimeException("Sản phẩm phải có ít nhất một biến thể");
			}

			// *** MỚI: TẠO PROMOTION NỘI BỘ (Product-Level Discount) ***
			if (request.getDiscountPercentage() != null && request.getDiscountPercentage() > 0) {
				createInternalProductPromotion(product, request.getDiscountPercentage(), shop, userId);
			}

			// Tạo yêu cầu phê duyệt
			ProductApproval approval = new ProductApproval();
			approval.setProduct(product);
			approval.setActionType("CREATE");
			approval.setStatus("Pending");
			approval.setChangeDetails(objectMapper.writeValueAsString(request));
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
		}

		@Transactional
		public void requestProductUpdate(Integer shopId, ProductRequestDTO request, Integer userId,
				Set<Topping> selectedToppings) throws Exception {

			Product product = getProductDetail(shopId, request.getProductId());
			User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

			List<ProductApproval> existingApprovals = productApprovalRepository
					.findByProduct_ProductIDAndStatus(product.getProductID(), "Pending");

			if (!existingApprovals.isEmpty()) {
				throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho sản phẩm này");
			}

			// Upload ảnh mới (nếu có)
			List<String> uploadedImageUrls = new ArrayList<>();
			boolean newImagesSubmitted = request.getImages() != null && !request.getImages().isEmpty()
					&& request.getImages().stream().anyMatch(f -> f != null && !f.isEmpty());

			if (newImagesSubmitted) {
				log.info("Processing new images for product update ID: {}", request.getProductId());
				for (MultipartFile file : request.getImages()) {
					if (file != null && !file.isEmpty()) {
						try {
							Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(file,
									"products", userId);
							String imageUrl = uploadResult.get("secure_url");
							if (imageUrl != null) {
								uploadedImageUrls.add(imageUrl);
							}
						} catch (Exception e) {
							throw new RuntimeException("Lỗi khi upload hình ảnh mới: " + e.getMessage());
						}
					}
				}
				if (!uploadedImageUrls.isEmpty()) {
					request.setNewImageUrls(uploadedImageUrls);
				}
			} else {
				request.setNewImageUrls(null);
			}

			// *** TÍNH LẠI basePrice NẾU CÓ THAY ĐỔI VARIANTS ***
			// (Logic này sẽ được xử lý khi approval được duyệt)
			// Nhưng bạn có thể thêm vào changeDetails để admin biết giá mới
			if (request.getVariants() != null && !request.getVariants().isEmpty()) {
				BigDecimal newMinPrice = request.getVariants().stream().map(ProductVariantDTO::getPrice)
						.filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null);

				// Thêm thông tin basePrice mới vào DTO (optional)
//				 request.setNewBasePrice(newMinPrice);

				log.info("New basePrice will be: {}", newMinPrice);
			}

			// Tạo yêu cầu phê duyệt
			ProductApproval approval = new ProductApproval();
			approval.setProduct(product);
			approval.setActionType("UPDATE");
			approval.setStatus("Pending");
			approval.setChangeDetails(objectMapper.writeValueAsString(request));
			approval.setRequestedBy(user);
			approval.setRequestedAt(LocalDateTime.now());
			approval = productApprovalRepository.save(approval);

			notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
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

			// Available toppings
			if (product.getAvailableToppings() != null) {
				Set<Integer> toppingIds = product.getAvailableToppings().stream().map(Topping::getToppingID)
						.collect(Collectors.toSet());
				dto.setAvailableToppingIds(toppingIds);
			}

			// *** THÊM: LOAD DISCOUNT PERCENTAGE ***
			if (product.getPromotionProducts() != null && !product.getPromotionProducts().isEmpty()) {
				product.getPromotionProducts().stream().filter(pp -> pp.getPromotion() != null)
						.filter(pp -> "PRODUCT".equals(pp.getPromotion().getPromotionType()))
						.filter(pp -> pp.getPromotion().getStatus() == 1).filter(pp -> {
							LocalDateTime now = LocalDateTime.now();
							return pp.getPromotion().getStartDate().isBefore(now)
									&& pp.getPromotion().getEndDate().isAfter(now);
						}).findFirst().ifPresent(pp -> {
							dto.setDiscountPercentage(pp.getDiscountPercentage());
							log.info("Loaded discount {}% for product {}", pp.getDiscountPercentage(),
									product.getProductID());
						});
			}

			return dto;
		}
}
