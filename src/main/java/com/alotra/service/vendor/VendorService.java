//package com.alotra.service.vendor;
//
//import com.alotra.dto.*;
//import com.alotra.dto.product.ProductRequestDTO;
//import com.alotra.dto.product.ProductStatisticsDTO;
//import com.alotra.dto.product.ProductVariantDTO;
//import com.alotra.dto.product.SimpleCategoryDTO;
//import com.alotra.dto.product.SimpleProductDTO;
//import com.alotra.dto.product.SimpleSizeDTO;
//import com.alotra.dto.promotion.PromotionStatisticsDTO;
//import com.alotra.dto.promotion.ProductDiscountDTO;
//import com.alotra.dto.promotion.PromotionRequestDTO;
//import com.alotra.dto.response.ApprovalResponseDTO;
//import com.alotra.dto.shop.CategoryRevenueDTO;
//import com.alotra.dto.shop.ShopDashboardDTO;
//import com.alotra.dto.shop.ShopEmployeeDTO;
//import com.alotra.dto.shop.ShopOrderDTO;
//import com.alotra.dto.shop.ShopProfileDTO;
//import com.alotra.dto.shop.ShopRevenueDTO;
//import com.alotra.dto.topping.ToppingRequestDTO;
//import com.alotra.dto.topping.ToppingStatisticsDTO;
//import com.alotra.entity.*;
//import com.alotra.entity.order.Order;
//import com.alotra.entity.order.OrderHistory;
//import com.alotra.entity.product.Category;
//import com.alotra.entity.product.Product;
//import com.alotra.entity.product.ProductApproval;
//import com.alotra.entity.product.ProductImage;
//import com.alotra.entity.product.ProductVariant;
//import com.alotra.entity.product.Size;
//import com.alotra.entity.product.Topping;
//import com.alotra.entity.product.ToppingApproval;
//import com.alotra.entity.promotion.Promotion;
//import com.alotra.entity.promotion.PromotionApproval;
//import com.alotra.entity.promotion.PromotionProduct;
//import com.alotra.entity.promotion.PromotionProductId;
//import com.alotra.entity.shop.Shop;
//import com.alotra.entity.shop.ShopEmployee;
//import com.alotra.entity.shop.ShopRevenue;
//import com.alotra.entity.user.Role;
//import com.alotra.entity.user.User;
//import com.alotra.repository.*;
//import com.alotra.repository.order.OrderHistoryRepository;
//import com.alotra.repository.order.OrderRepository;
//import com.alotra.repository.product.CategoryRepository;
//import com.alotra.repository.product.ProductApprovalRepository;
//import com.alotra.repository.product.ProductImageRepository;
//import com.alotra.repository.product.ProductRepository;
//import com.alotra.repository.product.ProductVariantRepository;
//import com.alotra.repository.product.SizeRepository;
//import com.alotra.repository.product.ToppingApprovalRepository;
//import com.alotra.repository.product.ToppingRepository;
//import com.alotra.repository.promotion.PromotionApprovalRepository;
//import com.alotra.repository.promotion.PromotionProductRepository;
//import com.alotra.repository.promotion.PromotionRepository;
//import com.alotra.repository.shop.ShopEmployeeRepository;
//import com.alotra.repository.shop.ShopRepository;
//import com.alotra.repository.shop.ShopRevenueRepository;
//import com.alotra.repository.user.RoleRepository;
//import com.alotra.repository.user.UserRepository;
//import com.alotra.service.cloudinary.CloudinaryService;
//import com.alotra.service.notification.NotificationService;
//import com.alotra.service.order.ShipperOrderService;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import jakarta.persistence.TypedQuery;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//@Transactional
//public class VendorService {
//
//	private final ShopRepository shopRepository;
//	private final ProductRepository productRepository;
//	private final ProductApprovalRepository productApprovalRepository;
//	private final PromotionRepository promotionRepository;
//	private final PromotionProductRepository promotionProductRepository;
//	private final PromotionApprovalRepository promotionApprovalRepository;
//	private final OrderRepository orderRepository;
//	private final ShopRevenueRepository shopRevenueRepository;
//	private final ProductVariantRepository productVariantRepository;
//	private final ProductImageRepository productImageRepository;
//	private final SizeRepository sizeRepository;
//	private final CategoryRepository categoryRepository;
//	private final CloudinaryService cloudinaryService;
//	private final NotificationService notificationService;
//	private final UserRepository userRepository;
//	private final ToppingRepository toppingRepository;
//	private final ToppingApprovalRepository toppingApprovalRepository;
//	private final ShipperOrderService shipperOrderService;
//	private final OrderHistoryRepository orderHistoryRepository;
//
//	private final ObjectMapper objectMapper;
//	@PersistenceContext // Inject EntityManager for JPQL
//	private EntityManager entityManager;
//
//	// ==================== DASHBOARD ====================
//
//	public ShopDashboardDTO getShopDashboard(Integer shopId) {
//		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
//
//		ShopDashboardDTO dashboard = new ShopDashboardDTO();
//		dashboard.setShopId(shopId);
//		dashboard.setShopName(shop.getShopName());
//		dashboard.setLogoUrl(shop.getLogoURL());
//
//		// Thống kê sản phẩm
//		dashboard.setTotalProducts(productRepository.countByShopIdAndStatus(shopId, null).intValue());
//		dashboard.setActiveProducts(productRepository.countByShopIdAndStatus(shopId, (byte) 1).intValue());
//
//		// Thống kê phê duyệt đang chờ
//		Long pendingProducts = productApprovalRepository.countPendingByShopId(shopId);
//		Long pendingPromotions = promotionApprovalRepository.countPendingByShopId(shopId);
//		Long pendingToppings = toppingApprovalRepository.countPendingByShopId(shopId);
//		dashboard.setPendingApprovals((int) (pendingProducts + pendingPromotions + pendingToppings));
//
//		// Thống kê đơn hàng
//		dashboard.setTotalOrders(orderRepository.countByShopId(shopId));
//		dashboard.setPendingOrders(orderRepository.countByShopIdAndStatus(shopId, "Pending"));
//		dashboard.setDeliveringOrders(orderRepository.countByShopIdAndStatus(shopId, "Delivering"));
//
//		// Thống kê doanh thu
//		Double totalRevenue = shopRevenueRepository.getTotalRevenueByShopId(shopId);
//		dashboard.setTotalRevenue(BigDecimal.valueOf(totalRevenue != null ? totalRevenue : 0));
//
//		LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
//		LocalDateTime endOfMonth = LocalDateTime.now();
//		Double monthRevenue = shopRevenueRepository.getRevenueByShopIdAndDateRange(shopId, startOfMonth, endOfMonth);
//		dashboard.setThisMonthRevenue(BigDecimal.valueOf(monthRevenue != null ? monthRevenue : 0));
//
//		return dashboard;
//	}
//
//	// ==================== PRODUCT MANAGEMENT ====================
//
//	public Page<ProductStatisticsDTO> getShopProducts(Integer shopId, Byte status, Integer categoryId,
//			String approvalStatus, String search, Pageable pageable) {
//
//		String normalizedSearch = (search != null && search.trim().isEmpty()) ? null : search;
//		String normalizedApprovalStatus = (approvalStatus != null && approvalStatus.trim().isEmpty()) ? null
//				: approvalStatus;
//
//		log.debug("Calling repository with: shopId={}, status={}, categoryId={}, approvalStatus='{}', search='{}'",
//				shopId, status, categoryId, normalizedApprovalStatus, normalizedSearch);
//
//		Page<Product> products = productRepository.searchShopProducts(shopId, status, categoryId,
//				normalizedApprovalStatus, normalizedSearch, pageable);
//
//		return products.map(product -> {
//			ProductStatisticsDTO dto = new ProductStatisticsDTO();
//			dto.setProductId(product.getProductID());
//			dto.setProductName(product.getProductName());
//			dto.setSoldCount(product.getSoldCount());
//			dto.setAverageRating(product.getAverageRating());
//			dto.setTotalReviews(product.getTotalReviews());
//			dto.setViewCount(product.getViewCount());
//			dto.setStatus(product.getStatus() == 1 ? "Đang hoạt động" : "Không hoạt động");
//
//			// Primary image
//			product.getImages().stream().filter(img -> img != null && Boolean.TRUE.equals(img.getIsPrimary()))
//					.findFirst().ifPresent(img -> dto.setPrimaryImageUrl(img.getImageURL()));
//
//			// Min price
//			product.getVariants().stream().filter(Objects::nonNull).map(ProductVariant::getPrice)
//					.filter(Objects::nonNull).min(BigDecimal::compareTo).ifPresent(dto::setMinPrice);
//
//			// *** THÊM: TÌM DISCOUNT PERCENTAGE ***
//			// Tìm promotion nội bộ của product (PromotionType = "PRODUCT")
//			if (product.getPromotionProducts() != null && !product.getPromotionProducts().isEmpty()) {
//				product.getPromotionProducts().stream().filter(pp -> pp.getPromotion() != null)
//						.filter(pp -> "PRODUCT".equals(pp.getPromotion().getPromotionType()))
//						.filter(pp -> pp.getPromotion().getStatus() == 1) // Chỉ lấy promotion đang active
//						.filter(pp -> {
//							// Kiểm tra còn hiệu lực
//							LocalDateTime now = LocalDateTime.now();
//							return pp.getPromotion().getStartDate().isBefore(now)
//									&& pp.getPromotion().getEndDate().isAfter(now);
//						}).findFirst().ifPresent(pp -> dto.setDiscountPercentage(pp.getDiscountPercentage()));
//			}
//
//			// Approval status
//			Optional<ProductApproval> latestApprovalOpt = productApprovalRepository
//					.findTopByProduct_ProductIDOrderByRequestedAtDesc(product.getProductID());
//
//			if (latestApprovalOpt.isPresent()) {
//				ProductApproval latestApproval = latestApprovalOpt.get();
//				String currentApprovalDbStatus = latestApproval.getStatus();
//
//				if ("Pending".equals(currentApprovalDbStatus) || "Rejected".equals(currentApprovalDbStatus)) {
//					String actionTypeText = "";
//					switch (latestApproval.getActionType()) {
//					case "CREATE":
//						actionTypeText = "Tạo mới";
//						break;
//					case "UPDATE":
//						actionTypeText = "Cập nhật";
//						break;
//					case "DELETE":
//						actionTypeText = "Xóa";
//						break;
//					default:
//						actionTypeText = latestApproval.getActionType();
//					}
//
//					if ("Pending".equals(currentApprovalDbStatus)) {
//						dto.setApprovalStatus("Đang chờ: " + actionTypeText);
//					} else {
//						dto.setApprovalStatus("Bị từ chối: " + actionTypeText);
//					}
//				}
//			}
//
//			return dto;
//		});
//	}
//
//	public Product getProductDetail(Integer shopId, Integer productId) {
//		Product product = productRepository.findById(productId)
//				.orElseThrow(() -> new RuntimeException("Product not found"));
//
//		if (!product.getShop().getShopId().equals(shopId)) {
//			throw new RuntimeException("Unauthorized: Product does not belong to this shop");
//		}
//
//		return product;
//	}
//
//	@Transactional
//	public void requestProductCreation(Integer shopId, ProductRequestDTO request, Integer userId,
//			Set<Topping> selectedToppings) throws JsonProcessingException {
//
//		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
//
//		// Tạo sản phẩm mới với status = 0 (Inactive)
//		Product product = new Product();
//		product.setShop(shop);
//		product.setCategory(categoryRepository.findById(request.getCategoryId())
//				.orElseThrow(() -> new RuntimeException("Category not found")));
//		product.setProductName(request.getProductName());
//		product.setDescription(request.getDescription());
//		product.setStatus((byte) 0);
//		product.setCreatedAt(LocalDateTime.now());
//		product.setUpdatedAt(LocalDateTime.now());
//		product.setAvailableToppings(selectedToppings);
//
//		// *** LƯU SẢN PHẨM TRƯỚC (để có ID) ***
//		product = productRepository.save(product);
//		log.info("Product entity created with ID: {}", product.getProductID());
//
//		// Upload ảnh (giữ nguyên logic cũ)
//		if (request.getImages() != null && !request.getImages().isEmpty()) {
//			boolean hasValidImage = request.getImages().stream().anyMatch(file -> file != null && !file.isEmpty());
//			if (hasValidImage) {
//				log.info("Processing images for new product ID: {}", product.getProductID());
//				for (int i = 0; i < request.getImages().size(); i++) {
//					MultipartFile file = request.getImages().get(i);
//					if (file != null && !file.isEmpty()) {
//						try {
//							Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(file,
//									"products", userId);
//							String imageUrl = uploadResult.get("secure_url");
//							if (imageUrl == null) {
//								throw new RuntimeException("Lỗi khi upload hình ảnh: Không nhận được URL.");
//							}
//							ProductImage productImage = new ProductImage();
//							productImage.setProduct(product);
//							productImage.setImageURL(imageUrl);
//							productImage.setIsPrimary(
//									i == (request.getPrimaryImageIndex() != null ? request.getPrimaryImageIndex() : 0));
//							productImage.setDisplayOrder(i);
//							productImageRepository.save(productImage);
//							log.info("Image uploaded and saved: {}", imageUrl);
//						} catch (Exception e) {
//							log.error("Error uploading image at index {}: {}", i, e.getMessage(), e);
//							throw new RuntimeException("Lỗi khi upload hình ảnh: " + e.getMessage());
//						}
//					}
//				}
//			} else {
//				throw new RuntimeException("Vui lòng upload ít nhất một hình ảnh hợp lệ.");
//			}
//		} else {
//			throw new RuntimeException("Vui lòng upload ít nhất một hình ảnh.");
//		}
//
//		// *** TẠO VARIANTS VÀ TỰ ĐỘNG TÍNH basePrice ***
//		if (request.getVariants() != null && !request.getVariants().isEmpty()) {
//			List<ProductVariant> createdVariants = new ArrayList<>();
//
//			for (ProductVariantDTO variantDTO : request.getVariants()) {
//				ProductVariant variant = new ProductVariant();
//				variant.setProduct(product);
//				variant.setSize(sizeRepository.findById(variantDTO.getSizeId())
//						.orElseThrow(() -> new RuntimeException("Size not found")));
//				variant.setPrice(variantDTO.getPrice());
//				variant.setStock(variantDTO.getStock());
//				variant.setSku(variantDTO.getSku());
//
//				variant = productVariantRepository.save(variant);
//				createdVariants.add(variant);
//
//				log.info("Variant saved: Size={}, Price={}, Stock={}", variantDTO.getSizeId(), variantDTO.getPrice(),
//						variantDTO.getStock());
//			}
//
//			// *** TÍNH VÀ LƯU basePrice ***
//			BigDecimal minPrice = createdVariants.stream().map(ProductVariant::getPrice).filter(Objects::nonNull)
//					.min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
//
//			product.setBasePrice(minPrice);
//			product = productRepository.save(product); // Lưu lại để cập nhật basePrice
//
//			log.info("Product basePrice calculated and saved: {}", minPrice);
//
//		} else {
//			throw new RuntimeException("Sản phẩm phải có ít nhất một biến thể");
//		}
//
//		// *** MỚI: TẠO PROMOTION NỘI BỘ (Product-Level Discount) ***
//		if (request.getDiscountPercentage() != null && request.getDiscountPercentage() > 0) {
//			createInternalProductPromotion(product, request.getDiscountPercentage(), shop, userId);
//		}
//
//		// Tạo yêu cầu phê duyệt
//		ProductApproval approval = new ProductApproval();
//		approval.setProduct(product);
//		approval.setActionType("CREATE");
//		approval.setStatus("Pending");
//		approval.setChangeDetails(objectMapper.writeValueAsString(request));
//		approval.setRequestedBy(
//				userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
//		approval.setRequestedAt(LocalDateTime.now());
//
//		approval = productApprovalRepository.save(approval);
//		log.info("Product approval created with ID: {}", approval.getApprovalId());
//
//		// Gửi thông báo
//		try {
//			notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
//		} catch (Exception e) {
//			log.error("Error sending notification: {}", e.getMessage());
//		}
//	}
//
//	@Transactional
//	public void requestProductUpdate(Integer shopId, ProductRequestDTO request, Integer userId,
//			Set<Topping> selectedToppings) throws Exception {
//
//		Product product = getProductDetail(shopId, request.getProductId());
//		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//		List<ProductApproval> existingApprovals = productApprovalRepository
//				.findByProduct_ProductIDAndStatus(product.getProductID(), "Pending");
//
//		if (!existingApprovals.isEmpty()) {
//			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho sản phẩm này");
//		}
//
//		// Upload ảnh mới (nếu có)
//		List<String> uploadedImageUrls = new ArrayList<>();
//		boolean newImagesSubmitted = request.getImages() != null && !request.getImages().isEmpty()
//				&& request.getImages().stream().anyMatch(f -> f != null && !f.isEmpty());
//
//		if (newImagesSubmitted) {
//			log.info("Processing new images for product update ID: {}", request.getProductId());
//			for (MultipartFile file : request.getImages()) {
//				if (file != null && !file.isEmpty()) {
//					try {
//						Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(file,
//								"products", userId);
//						String imageUrl = uploadResult.get("secure_url");
//						if (imageUrl != null) {
//							uploadedImageUrls.add(imageUrl);
//						}
//					} catch (Exception e) {
//						throw new RuntimeException("Lỗi khi upload hình ảnh mới: " + e.getMessage());
//					}
//				}
//			}
//			if (!uploadedImageUrls.isEmpty()) {
//				request.setNewImageUrls(uploadedImageUrls);
//			}
//		} else {
//			request.setNewImageUrls(null);
//		}
//
//		// *** TÍNH LẠI basePrice NẾU CÓ THAY ĐỔI VARIANTS ***
//		// (Logic này sẽ được xử lý khi approval được duyệt)
//		// Nhưng bạn có thể thêm vào changeDetails để admin biết giá mới
//		if (request.getVariants() != null && !request.getVariants().isEmpty()) {
//			BigDecimal newMinPrice = request.getVariants().stream().map(ProductVariantDTO::getPrice)
//					.filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null);
//
//			// Thêm thông tin basePrice mới vào DTO (optional)
////			 request.setNewBasePrice(newMinPrice);
//
//			log.info("New basePrice will be: {}", newMinPrice);
//		}
//
//		// Tạo yêu cầu phê duyệt
//		ProductApproval approval = new ProductApproval();
//		approval.setProduct(product);
//		approval.setActionType("UPDATE");
//		approval.setStatus("Pending");
//		approval.setChangeDetails(objectMapper.writeValueAsString(request));
//		approval.setRequestedBy(user);
//		approval.setRequestedAt(LocalDateTime.now());
//		approval = productApprovalRepository.save(approval);
//
//		notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
//		log.info("Product update requested - Product ID: {}, Shop ID: {}, Approval ID: {}", product.getProductID(),
//				shopId, approval.getApprovalId());
//	}
//
//	public void requestProductDeletion(Integer shopId, Integer productId, Integer userId) {
//		Product product = productRepository.findById(productId)
//				.orElseThrow(() -> new RuntimeException("Product not found"));
//
//		if (!product.getShop().getShopId().equals(shopId)) {
//			throw new RuntimeException("Unauthorized: Product does not belong to this shop");
//		}
//
//		// Kiểm tra pending requests
//		List<ProductApproval> existingApprovals = productApprovalRepository
//				.findByProduct_ProductIDAndStatus(product.getProductID(), "Pending");
//
//		if (!existingApprovals.isEmpty()) {
//			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho sản phẩm này");
//		}
//
//		// Tạo yêu cầu phê duyệt
//		ProductApproval approval = new ProductApproval();
//		approval.setProduct(product);
//		approval.setActionType("DELETE");
//		approval.setStatus("Pending");
//		approval.setRequestedBy(
//				userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
//		approval.setRequestedAt(LocalDateTime.now());
//
//		approval = productApprovalRepository.save(approval);
//
//		log.info("Product approval created with ID: {}", approval.getApprovalId());
//
//		// Gửi thông báo
//		try {
//			notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
//		} catch (Exception e) {
//			log.error("Error sending notification: {}", e.getMessage());
//		}
//
//		log.info("Product deletion requested - Product ID: {}, Shop ID: {}, Approval ID: {}", product.getProductID(),
//				shopId, approval.getApprovalId());
//	}
//
//	public List<SimpleCategoryDTO> getAllCategoriesSimple() {
//		return categoryRepository.findAll().stream()
//				.map(c -> new SimpleCategoryDTO(c.getCategoryID(), c.getCategoryName())).collect(Collectors.toList());
//	}
//
//	public List<SimpleSizeDTO> getAllSizesSimple() {
//		return sizeRepository.findAll().stream().map(s -> new SimpleSizeDTO(s.getSizeID(), s.getSizeName()))
//				.collect(Collectors.toList());
//	}
//
//	public List<Category> getAllCategories() {
//		return categoryRepository.findAll();
//	}
//
//	public List<Size> getAllSizes() {
//		return sizeRepository.findAll();
//	}
//
//	public ProductRequestDTO convertProductToDTO(Product product) {
//		ProductRequestDTO dto = new ProductRequestDTO();
//
//		dto.setProductId(product.getProductID());
//		dto.setProductName(product.getProductName());
//		dto.setDescription(product.getDescription());
//		dto.setCategoryId(product.getCategory().getCategoryID());
//
//		// Convert variants
//		List<ProductVariantDTO> variantDTOs = new ArrayList<>();
//		for (ProductVariant variant : product.getVariants()) {
//			ProductVariantDTO variantDTO = new ProductVariantDTO();
//			variantDTO.setVariantId(variant.getVariantID());
//			variantDTO.setSizeId(variant.getSize().getSizeID());
//			variantDTO.setPrice(variant.getPrice());
//			variantDTO.setStock(variant.getStock());
//			variantDTO.setSku(variant.getSku());
//			variantDTOs.add(variantDTO);
//		}
//		dto.setVariants(variantDTOs);
//
//		// Find primary image index
//		List<ProductImage> images = product.getImages().stream()
//				.sorted(Comparator.comparing(ProductImage::getDisplayOrder)).collect(Collectors.toList());
//
//		for (int i = 0; i < images.size(); i++) {
//			if (images.get(i).getIsPrimary()) {
//				dto.setPrimaryImageIndex(i);
//				break;
//			}
//		}
//
//		// Available toppings
//		if (product.getAvailableToppings() != null) {
//			Set<Integer> toppingIds = product.getAvailableToppings().stream().map(Topping::getToppingID)
//					.collect(Collectors.toSet());
//			dto.setAvailableToppingIds(toppingIds);
//		}
//
//		// *** THÊM: LOAD DISCOUNT PERCENTAGE ***
//		if (product.getPromotionProducts() != null && !product.getPromotionProducts().isEmpty()) {
//			product.getPromotionProducts().stream().filter(pp -> pp.getPromotion() != null)
//					.filter(pp -> "PRODUCT".equals(pp.getPromotion().getPromotionType()))
//					.filter(pp -> pp.getPromotion().getStatus() == 1).filter(pp -> {
//						LocalDateTime now = LocalDateTime.now();
//						return pp.getPromotion().getStartDate().isBefore(now)
//								&& pp.getPromotion().getEndDate().isAfter(now);
//					}).findFirst().ifPresent(pp -> {
//						dto.setDiscountPercentage(pp.getDiscountPercentage());
//						log.info("Loaded discount {}% for product {}", pp.getDiscountPercentage(),
//								product.getProductID());
//					});
//		}
//
//		return dto;
//	}
//
//	// ==================== TOPPING MANAGEMENT ====================
//
//	public Page<ToppingStatisticsDTO> getShopToppings(Integer shopId, Byte status, String search, Pageable pageable) {
//		Page<Topping> toppings = toppingRepository.findShopToppingsFiltered(shopId, status, search, pageable);
//
//		return toppings.map(topping -> {
//			String approvalStatus = null;
//			String activityStatus;
//
//			Optional<ToppingApproval> latestApprovalOpt = toppingApprovalRepository
//					.findTopByTopping_ToppingIDOrderByRequestedAtDesc(topping.getToppingID());
//
//			if (latestApprovalOpt.isPresent()) {
//				ToppingApproval latestApproval = latestApprovalOpt.get();
//				String currentDbStatus = latestApproval.getStatus();
//
//				if ("Pending".equals(currentDbStatus) || "Rejected".equals(currentDbStatus)) {
//					String actionTypeText = "";
//					switch (latestApproval.getActionType()) {
//					case "CREATE":
//						actionTypeText = "Tạo mới";
//						break;
//					case "UPDATE":
//						actionTypeText = "Cập nhật";
//						break;
//					case "DELETE":
//						actionTypeText = "Xóa";
//						break;
//					default:
//						actionTypeText = latestApproval.getActionType();
//					}
//					approvalStatus = ("Pending".equals(currentDbStatus) ? "Đang chờ: " : "Bị từ chối: ")
//							+ actionTypeText;
//				}
//			}
//
//			activityStatus = (topping.getStatus() == 1) ? "Đang hoạt động" : "Không hoạt động";
//
//			return new ToppingStatisticsDTO(topping, approvalStatus, activityStatus);
//		});
//	}
//
//	public void requestToppingCreation(Integer shopId, ToppingRequestDTO request, Integer userId) throws Exception { // Ném
//																														// Exception
//		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
//		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//		String uploadedImageUrl = null;
//		// *** THÊM LOGIC UPLOAD ẢNH ***
//		if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
//			try {
//				// Upload lên Cloudinary vào thư mục "toppings"
//				Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(request.getImageFile(),
//						"toppings", userId);
//				uploadedImageUrl = uploadResult.get("secure_url");
//			} catch (Exception e) {
//				log.error("Lỗi upload ảnh topping: {}", e.getMessage(), e);
//				throw new RuntimeException("Lỗi khi upload hình ảnh topping: " + e.getMessage());
//			}
//		}
//		// *** KẾT THÚC LOGIC UPLOAD ẢNH ***
//
//		// Tạo Topping mới với Status = 0 (Inactive)
//		Topping topping = new Topping();
//		topping.setShop(shop);
//		topping.setToppingName(request.getToppingName());
//		topping.setAdditionalPrice(request.getAdditionalPrice());
//		topping.setImageURL(uploadedImageUrl); // *** SỬA: Gán URL đã upload ***
//		topping.setStatus((byte) 0); // Chờ duyệt
//
//		topping = toppingRepository.save(topping); // Lưu topping để lấy ID
//
//		// Tạo yêu cầu phê duyệt
//		ToppingApproval approval = new ToppingApproval();
//		approval.setTopping(topping);
//		approval.setShop(shop);
//		approval.setActionType("CREATE");
//		approval.setStatus("Pending");
//
//		request.setImageURL(uploadedImageUrl); // *** THÊM: Gán URL vào DTO trước khi lưu JSON ***
//		approval.setChangeDetails(objectMapper.writeValueAsString(request));
//
//		approval.setRequestedBy(user);
//
//		toppingApprovalRepository.save(approval);
//
//		notificationService.notifyAdminsAboutNewApproval("TOPPING", topping.getToppingID());
//	}
//
//	public Topping getToppingDetail(Integer shopId, Integer toppingId) {
//		Topping topping = toppingRepository.findById(toppingId)
//				.orElseThrow(() -> new RuntimeException("Topping not found"));
//		if (!topping.getShop().getShopId().equals(shopId)) {
//			throw new RuntimeException("Unauthorized: Topping does not belong to this shop");
//		}
//		return topping;
//	}
//
//	public ToppingRequestDTO convertToppingToDTO(Topping topping) {
//		ToppingRequestDTO dto = new ToppingRequestDTO();
//		dto.setToppingId(topping.getToppingID());
//		dto.setToppingName(topping.getToppingName());
//		dto.setAdditionalPrice(topping.getAdditionalPrice());
//		dto.setImageURL(topping.getImageURL());
//		return dto;
//	}
//
//	public void requestToppingUpdate(Integer shopId, ToppingRequestDTO request, Integer userId) throws Exception { // Ném
//																													// Exception
//		Topping topping = getToppingDetail(shopId, request.getToppingId()); // Kiểm tra quyền sở hữu
//		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//		List<ToppingApproval> existingApprovals = toppingApprovalRepository
//				.findByTopping_ToppingIDAndStatus(topping.getToppingID(), "Pending");
//		if (!existingApprovals.isEmpty()) {
//			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho topping này");
//		}
//
//		// *** THÊM LOGIC UPLOAD ẢNH (CHO UPDATE) ***
//		if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
//			// Nếu có file mới, upload và set URL mới cho DTO
//			try {
//				Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(request.getImageFile(),
//						"toppings", userId);
//				request.setImageURL(uploadResult.get("secure_url")); // Gán URL mới vào DTO
//			} catch (Exception e) {
//				log.error("Lỗi upload ảnh topping (update): {}", e.getMessage(), e);
//				throw new RuntimeException("Lỗi khi upload hình ảnh topping: " + e.getMessage());
//			}
//		} else {
//			// Nếu không có file mới, giữ lại URL ảnh cũ từ database
//			request.setImageURL(topping.getImageURL());
//		}
//		// *** KẾT THÚC LOGIC UPLOAD ẢNH ***
//
//		ToppingApproval approval = new ToppingApproval();
//		approval.setTopping(topping);
//		approval.setShop(topping.getShop());
//		approval.setActionType("UPDATE");
//		approval.setStatus("Pending");
//		approval.setChangeDetails(objectMapper.writeValueAsString(request)); // Lưu thay đổi (đã bao gồm imageURL)
//		approval.setRequestedBy(user);
//
//		toppingApprovalRepository.save(approval);
//		notificationService.notifyAdminsAboutNewApproval("TOPPING", topping.getToppingID());
//	}
//
//	public void requestToppingDeletion(Integer shopId, Integer toppingId, Integer userId) {
//		Topping topping = getToppingDetail(shopId, toppingId); // Kiểm tra quyền sở hữu
//		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//		List<ToppingApproval> existingApprovals = toppingApprovalRepository
//				.findByTopping_ToppingIDAndStatus(topping.getToppingID(), "Pending");
//		if (!existingApprovals.isEmpty()) {
//			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho topping này");
//		}
//
//		ToppingApproval approval = new ToppingApproval();
//		approval.setTopping(topping);
//		approval.setShop(topping.getShop());
//		approval.setActionType("DELETE");
//		approval.setStatus("Pending");
//		approval.setRequestedBy(user);
//
//		toppingApprovalRepository.save(approval);
//		// (Tùy chọn) Gửi thông báo
//	}
//
//	// ==================== PROMOTION MANAGEMENT ====================
//
//	public Page<PromotionStatisticsDTO> getShopPromotions(Integer shopId, Byte status, String promotionType,
//			Pageable pageable) {
//
//		// *** ĐÃ SỬA: Gọi phương thức repository mới và truyền LocalDateTime.now() ***
//		LocalDateTime now = LocalDateTime.now();
//		Page<Promotion> promotions = promotionRepository.findShopPromotionsFiltered(shopId, status, promotionType, now,
//				pageable);
//		// *** KẾT THÚC SỬA ***
//
//		// Phần map sang PromotionListDTO giữ nguyên như trước
//		return promotions.map(promotion -> {
//			String approvalStatus = null;
//			String activityStatus;
//
//			Optional<PromotionApproval> latestApprovalOpt = promotionApprovalRepository
//					.findTopByPromotion_PromotionIdOrderByRequestedAtDesc(promotion.getPromotionId());
//
//			if (latestApprovalOpt.isPresent()) {
//				PromotionApproval latestApproval = latestApprovalOpt.get();
//				String currentDbStatus = latestApproval.getStatus();
//
//				if ("Pending".equals(currentDbStatus) || "Rejected".equals(currentDbStatus)) {
//					String actionTypeText = "";
//					switch (latestApproval.getActionType()) {
//					case "CREATE":
//						actionTypeText = "Tạo mới";
//						break;
//					case "UPDATE":
//						actionTypeText = "Cập nhật";
//						break;
//					case "DELETE":
//						actionTypeText = "Xóa";
//						break;
//					default:
//						actionTypeText = latestApproval.getActionType();
//					}
//
//					if ("Pending".equals(currentDbStatus)) {
//						approvalStatus = "Đang chờ: " + actionTypeText;
//					} else {
//						approvalStatus = "Bị từ chối: " + actionTypeText;
//					}
//				}
//			}
//
//			// Tính toán Trạng thái Hoạt động (Dựa vào Status và EndDate)
//			// Logic này vẫn đúng vì nó tính toán sau khi đã lọc từ DB
//			if (promotion.getStatus() == 0) {
//				activityStatus = "Không hoạt động";
//			} else if (promotion.getEndDate().isBefore(now)) { // So sánh với 'now'
//				activityStatus = "Đã kết thúc";
//			} else {
//				activityStatus = "Đang hoạt động";
//			}
//
//			return new PromotionStatisticsDTO(promotion, approvalStatus, activityStatus);
//		});
//	}
//
//	public Promotion getPromotionDetail(Integer shopId, Integer promotionId) {
//		Promotion promotion = promotionRepository.findById(promotionId)
//				.orElseThrow(() -> new RuntimeException("Promotion not found"));
//
//		if (!promotion.getCreatedByShopID().getShopId().equals(shopId)) {
//			throw new RuntimeException("Unauthorized: Promotion does not belong to this shop");
//		}
//
//		return promotion;
//	}
//
//	public PromotionRequestDTO convertPromotionToDTO(Promotion promotion) {
//		PromotionRequestDTO dto = new PromotionRequestDTO();
//
//		dto.setPromotionId(promotion.getPromotionId());
//		dto.setPromotionName(promotion.getPromotionName());
//		dto.setDescription(promotion.getDescription());
//		dto.setPromoCode(promotion.getPromoCode());
//		dto.setDiscountType(promotion.getDiscountType());
//		dto.setDiscountValue(promotion.getDiscountValue());
//		dto.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
//		dto.setMinOrderValue(promotion.getMinOrderValue());
//		dto.setStartDate(promotion.getStartDate());
//		dto.setEndDate(promotion.getEndDate());
//		dto.setUsageLimit(promotion.getUsageLimit());
//
//		return dto;
//	}
//
//	public PromotionRequestDTO convertPromotionToDTOWithProducts(Promotion promotion) {
//		// 1. Chuyển đổi các trường cơ bản
//		PromotionRequestDTO dto = convertPromotionToDTO(promotion);
//
//		// 2. Nếu là loại "PRODUCT", tải các sản phẩm liên kết
//		if ("PRODUCT".equals(promotion.getPromotionType())) {
//			// Lấy danh sách PromotionProduct từ DB
//			List<PromotionProduct> promoProducts = promotionProductRepository.findByPromotion(promotion);
//
//			// Chuyển đổi sang List<ProductDiscountDTO>
//			if (promoProducts != null && !promoProducts.isEmpty()) {
//				List<ProductDiscountDTO> productDiscounts = promoProducts.stream().map(pp -> {
//					ProductDiscountDTO pdDTO = new ProductDiscountDTO();
//					pdDTO.setProductId(pp.getProduct().getProductID());
//					pdDTO.setDiscountPercentage(pp.getDiscountPercentage());
//					return pdDTO;
//				}).collect(Collectors.toList());
//				dto.setProductDiscounts(productDiscounts);
//			}
//		}
//		return dto;
//	}
//
//	@Transactional
//	public void requestPromotionCreation(Integer shopId, PromotionRequestDTO request, Integer userId)
//			throws JsonProcessingException {
//
//		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
//		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//		// Kiểm tra logic ngày tháng
//		if (request.getStartDate().isAfter(request.getEndDate())) {
//			throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
//		}
//
//		Promotion promotion = new Promotion();
//		promotion.setCreatedByUserID(user);
//		promotion.setCreatedByShopID(shop);
//		promotion.setPromotionName(request.getPromotionName());
//		promotion.setDescription(request.getDescription());
//		promotion.setPromoCode(request.getPromoCode());
//		promotion.setStartDate(request.getStartDate());
//		promotion.setEndDate(request.getEndDate());
//		promotion.setUsageLimit(request.getUsageLimit());
//		promotion.setPromotionType("ORDER"); // *** CHỈ CÒN ORDER ***
//		promotion.setStatus((byte) 0); // Pending
//
//		// *** CHỈ XỬ LÝ LOGIC ORDER ***
//		if (request.getDiscountType() == null || request.getDiscountValue() == null) {
//			throw new IllegalArgumentException("Loại giảm giá và Giá trị giảm giá là bắt buộc.");
//		}
//		promotion.setDiscountType(request.getDiscountType());
//		promotion.setDiscountValue(request.getDiscountValue());
//		promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
//		promotion.setMinOrderValue(request.getMinOrderValue());
//
//		Promotion savedPromotion = promotionRepository.save(promotion);
//
//		// *** BỎ LOGIC XỬ LÝ productDiscounts ***
//
//		// Tạo yêu cầu phê duyệt
//		PromotionApproval approval = new PromotionApproval();
//		approval.setPromotion(savedPromotion);
//		approval.setActionType("CREATE");
//		approval.setStatus("Pending");
//		approval.setChangeDetails(objectMapper.writeValueAsString(request));
//		approval.setRequestedBy(user);
//
//		promotionApprovalRepository.save(approval);
//		notificationService.notifyAdminsAboutNewApproval("PROMOTION", savedPromotion.getPromotionId());
//	}
//
//	public List<SimpleProductDTO> getShopProductsForSelection(Integer shopId) {
//		List<Product> products = productRepository.findActiveProductsByShop(shopId);
//		return products.stream().map(p -> new SimpleProductDTO(p.getProductID(), p.getProductName()))
//				.collect(Collectors.toList());
//	}
//
//	@Transactional
//	public void requestPromotionUpdate(Integer shopId, PromotionRequestDTO request, Integer userId) throws Exception {
//
//		Promotion promotion = getPromotionDetail(shopId, request.getPromotionId());
//		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//		// *** KIỂM TRA: KHÔNG CHO UPDATE PROMOTION PRODUCT TYPE ***
//		if ("PRODUCT".equals(promotion.getPromotionType())) {
//			throw new RuntimeException(
//					"Không thể chỉnh sửa khuyến mãi sản phẩm. Vui lòng sửa ở trang Quản lý sản phẩm.");
//		}
//
//		List<PromotionApproval> existingApprovals = promotionApprovalRepository
//				.findByPromotion_PromotionIdAndStatus(promotion.getPromotionId(), "Pending");
//		if (!existingApprovals.isEmpty()) {
//			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho khuyến mãi này");
//		}
//
//		if (request.getStartDate().isAfter(request.getEndDate())) {
//			throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
//		}
//
//		// *** CHỈ VALIDATE CHO ORDER TYPE ***
//		if (request.getDiscountType() == null || request.getDiscountValue() == null) {
//			throw new IllegalArgumentException("Loại giảm giá và Giá trị giảm giá là bắt buộc.");
//		}
//
//		// *** BỎ LOGIC productDiscounts ***
//
//		// Tạo yêu cầu phê duyệt
//		PromotionApproval approval = new PromotionApproval();
//		approval.setPromotion(promotion);
//		approval.setActionType("UPDATE");
//		approval.setStatus("Pending");
//		approval.setChangeDetails(objectMapper.writeValueAsString(request));
//		approval.setRequestedBy(user);
//		approval.setRequestedAt(LocalDateTime.now());
//
//		promotionApprovalRepository.save(approval);
//		notificationService.notifyAdminsAboutNewApproval("PROMOTION", promotion.getPromotionId());
//	}
//
//	public void requestPromotionDeletion(Integer shopId, Integer promotionId, Integer userId) {
//		Promotion promotion = promotionRepository.findById(promotionId)
//				.orElseThrow(() -> new RuntimeException("Promotion not found"));
//
//		if (!promotion.getCreatedByShopID().getShopId().equals(shopId)) {
//			throw new RuntimeException("Unauthorized: Promotion does not belong to this shop");
//		}
//
//		// *** ĐÃ THÊM: Kiểm tra pending requests (giống Product) ***
//		List<PromotionApproval> existingApprovals = promotionApprovalRepository
//				.findByPromotion_PromotionIdAndStatus(promotionId, "Pending");
//
//		if (!existingApprovals.isEmpty()) {
//			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho khuyến mãi này");
//		}
//
//		// Tạo yêu cầu phê duyệt
//		PromotionApproval approval = new PromotionApproval();
//		approval.setPromotion(promotion);
//		approval.setActionType("DELETE");
//		approval.setStatus("Pending");
//		approval.setRequestedBy(
//				userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
//		approval.setRequestedAt(LocalDateTime.now());
//
//		promotionApprovalRepository.save(approval);
//
//		notificationService.notifyAdminsAboutNewApproval("PROMOTION", promotion.getPromotionId());
//
//		log.info("Promotion deletion requested - Promotion ID: {}, Shop ID: {}", promotion.getPromotionId(), shopId);
//	}
//
//	// ==================== ORDER MANAGEMENT ====================
//	public Page<ShopOrderDTO> getShopOrders(Integer shopId, String status, String searchQuery, Pageable pageable) {
//
//		Page<Order> orders = orderRepository.findShopOrdersFiltered(shopId, status, searchQuery, pageable);
//
//		// Mapping logic remains the same
//		return orders.map(order -> {
//			ShopOrderDTO dto = new ShopOrderDTO();
//			dto.setOrderId(order.getOrderID());
//			dto.setOrderDate(order.getOrderDate());
//			dto.setOrderStatus(order.getOrderStatus());
//			dto.setPaymentMethod(order.getPaymentMethod());
//			dto.setPaymentStatus(order.getPaymentStatus());
//			dto.setGrandTotal(order.getGrandTotal());
//			// Use associated User entity for customer info
//			if (order.getUser() != null) {
//				dto.setCustomerName(order.getUser().getFullName());
//				dto.setCustomerPhone(order.getUser().getPhoneNumber()); // Assuming User has phoneNumber
//			} else {
//				dto.setCustomerName("N/A"); // Handle case where user might be null?
//				dto.setCustomerPhone("N/A");
//			}
//			dto.setRecipientName(order.getRecipientName());
//			dto.setRecipientPhone(order.getRecipientPhone());
//			dto.setShippingAddress(order.getShippingAddress());
//
//			if (order.getShipper() != null) {
//				dto.setShipperName(order.getShipper().getFullName());
//			}
//
//			// Calculate total items (safer way)
//			dto.setTotalItems(order.getOrderDetails() != null ? order.getOrderDetails().size() : 0);
//
//			return dto;
//		});
//	}
//
//	public Order getOrderDetail(Integer shopId, Integer orderId) {
//		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
//
//		if (!order.getShop().getShopId().equals(shopId)) {
//			throw new RuntimeException("Unauthorized: Order does not belong to this shop");
//		}
//
//		return order;
//	}
//
//	public void updateOrderStatus(Integer shopId, Integer orderId, String newStatus, Integer userId) {
//		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
//
//		if (!order.getShop().getShopId().equals(shopId)) {
//			throw new RuntimeException("Unauthorized: Order does not belong to this shop");
//		}
//
//		// Validate status transition
//		validateOrderStatusTransition(order.getOrderStatus(), newStatus);
//
//		String oldStatus = order.getOrderStatus();
//		order.setOrderStatus(newStatus);
//
//		if ("Completed".equals(newStatus)) {
//			order.setCompletedAt(LocalDateTime.now());
//		}
//
//		orderRepository.save(order);
//
//		// Gửi thông báo cho khách hàng
//		notificationService.notifyCustomerAboutOrderStatus(order.getUser().getId(), orderId, newStatus);
//
//		log.info("Order status updated - Order ID: {}, Old Status: {}, New Status: {}", orderId, oldStatus, newStatus);
//	}
//
//	private void validateOrderStatusTransition(String currentStatus, String newStatus) {
//		Map<String, List<String>> allowedTransitions = new HashMap<>();
//		allowedTransitions.put("Pending", Arrays.asList("Confirmed", "Cancelled"));
//		allowedTransitions.put("Confirmed", Arrays.asList("Delivering", "Cancelled"));
//		allowedTransitions.put("Delivering", Arrays.asList("Completed", "Returned"));
//
//		List<String> allowed = allowedTransitions.get(currentStatus);
//		if (allowed == null || !allowed.contains(newStatus)) {
//			throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + newStatus);
//		}
//	}
//
//	@Transactional
//	public void assignShipperToOrder(Integer shopId, Integer orderId, Integer shipperId, Integer userId) {
//	    // 1. Kiểm tra đơn hàng thuộc về shop
//	    Order order = orderRepository.findById(orderId)
//	            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
//	    
//	    if (!order.getShop().getShopId().equals(shopId)) {
//	        throw new RuntimeException("Đơn hàng không thuộc về shop của bạn");
//	    }
//	    
////	    // 2. Kiểm tra trạng thái đơn hàng
////	    if (!"Confirmed".equals(order.getOrderStatus())) {
////	        throw new RuntimeException("Chỉ có thể gán shipper cho đơn hàng đã xác nhận");
////	    }
//	    
//	    // 3. Kiểm tra đơn hàng chưa có shipper
//	    if (order.getShipper() != null) {
//	        throw new RuntimeException("Đơn hàng đã được gán cho shipper: " + order.getShipper().getFullName());
//	    }
//	    
//	    // 4. Kiểm tra shipper là employee của shop
//	    ShopEmployee shipperEmployee = shopEmployeeRepository
//	            .findByShop_ShopIdAndUser_Id(shopId, shipperId)
//	            .orElseThrow(() -> new RuntimeException("Shipper không phải là nhân viên của shop"));
//	    
//	    if (!"Active".equals(shipperEmployee.getStatus())) {
//	        throw new RuntimeException("Shipper không còn hoạt động");
//	    }
//	    
//	    // 5. Kiểm tra user có role SHIPPER không
//	    boolean isShipper = shipperEmployee.getUser().getRoles().stream()
//	            .anyMatch(role -> "SHIPPER".equals(role.getRoleName()));
//	    
//	    if (!isShipper) {
//	        throw new RuntimeException("Nhân viên này không phải là shipper");
//	    }
//	    
//	    // 6. Gán shipper cho đơn hàng
//	    order.setShipper(shipperEmployee.getUser());
//	    order.setOrderStatus("Delivering"); // Chuyển sang trạng thái đang giao
//	    orderRepository.save(order);
//	    
//	    // 7. Tạo lịch sử giao hàng ban đầu
//	    shipperOrderService.createInitialShippingHistory(
//	            orderId, 
//	            shipperId, 
//	            "Đơn hàng đã được gán cho shipper: " + shipperEmployee.getUser().getFullName()
//	    );
//	    
//	    // 8. Lưu lịch sử thay đổi đơn hàng
//	    OrderHistory history = new OrderHistory();
//	    history.setOrder(order);
////	    history.setOldStatus("Confirmed");
////	    history.setNewStatus("Delivering");
//	    history.setChangedByUser(userRepository.findById(userId).orElse(null));
//	    history.setNotes("Gán shipper: " + shipperEmployee.getUser().getFullName());
//	    history.setTimestamp(LocalDateTime.now());
//	    orderHistoryRepository.save(history);
//	    
//	    log.info("Assigned shipper {} to order {}", shipperId, orderId);
//	}
//
//	/**
//	 * Lấy danh sách shipper có thể gán
//	 */
//	@Transactional(readOnly = true)
//	public List<ShopEmployeeDTO> getAvailableShippers(Integer shopId) {
//	    List<ShopEmployee> activeEmployees = shopEmployeeRepository
//	            .findByShop_ShopIdAndStatus(shopId, "Active");
//	    
//	    return activeEmployees.stream()
//	            .filter(emp -> emp.getUser().getRoles().stream()
//	                    .anyMatch(role -> "SHIPPER".equals(role.getRoleName())))
//	            .map(emp -> {
//	                ShopEmployeeDTO dto = new ShopEmployeeDTO();
//	                dto.setEmployeeId(emp.getEmployeeId());
//	                dto.setUserId(emp.getUser().getId());
//	                dto.setFullName(emp.getUser().getFullName());
//	                dto.setEmail(emp.getUser().getEmail());
//	                dto.setPhoneNumber(emp.getUser().getPhoneNumber());
//	                dto.setAvatarURL(emp.getUser().getAvatarURL());
//	                dto.setStatus(emp.getStatus());
//	                dto.setRoleName("SHIPPER");
//	                return dto;
//	            })
//	            .collect(Collectors.toList());
//	}
//
//	/**
//	 * Thay đổi shipper cho đơn hàng
//	 */
//	@Transactional
//	public void reassignShipper(Integer shopId, Integer orderId, Integer newShipperId, Integer userId, String reason) {
//	    Order order = orderRepository.findById(orderId)
//	            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
//	    
//	    if (!order.getShop().getShopId().equals(shopId)) {
//	        throw new RuntimeException("Đơn hàng không thuộc về shop của bạn");
//	    }
//	    
//	    if (!"Delivering".equals(order.getOrderStatus())) {
//	        throw new RuntimeException("Chỉ có thể thay đổi shipper khi đơn hàng đang giao");
//	    }
//	    
//	    User oldShipper = order.getShipper();
//	    if (oldShipper == null) {
//	        throw new RuntimeException("Đơn hàng chưa được gán shipper");
//	    }
//	    
//	    ShopEmployee newShipperEmployee = shopEmployeeRepository
//	            .findByShop_ShopIdAndUser_Id(shopId, newShipperId)
//	            .orElseThrow(() -> new RuntimeException("Shipper mới không phải là nhân viên của shop"));
//	    
//	    if (!"Active".equals(newShipperEmployee.getStatus())) {
//	        throw new RuntimeException("Shipper mới không còn hoạt động");
//	    }
//	    
//	    boolean isShipper = newShipperEmployee.getUser().getRoles().stream()
//	            .anyMatch(role -> "SHIPPER".equals(role.getRoleName()));
//	    
//	    if (!isShipper) {
//	        throw new RuntimeException("Nhân viên này không phải là shipper");
//	    }
//	    
//	    order.setShipper(newShipperEmployee.getUser());
//	    orderRepository.save(order);
//	    
//	    shipperOrderService.createInitialShippingHistory(
//	            orderId,
//	            newShipperId,
//	            "Được gán lại từ shipper " + oldShipper.getFullName() + ". Lý do: " + reason
//	    );
//	    
//	    OrderHistory history = new OrderHistory();
//	    history.setOrder(order);
//	    history.setOldStatus("Delivering");
//	    history.setNewStatus("Delivering");
//	    history.setChangedByUser(userRepository.findById(userId).orElse(null));
//	    history.setNotes("Thay đổi shipper từ " + oldShipper.getFullName() + 
//	                    " sang " + newShipperEmployee.getUser().getFullName() + 
//	                    ". Lý do: " + reason);
//	    history.setTimestamp(LocalDateTime.now());
//	    orderHistoryRepository.save(history);
//	    
//	    log.info("Reassigned order {} from shipper {} to shipper {}", 
//	            orderId, oldShipper.getId(), newShipperId);
//	}
//
//	
//	// ==================== REVENUE MANAGEMENT ====================
//
//	public List<ShopRevenueDTO> getShopRevenue(Integer shopId, LocalDateTime startDate, LocalDateTime endDate) {
//		// Nếu không có filter, mặc định lấy 14 ngày gần nhất
//		if (startDate == null && endDate == null) {
//			endDate = LocalDateTime.now();
//			startDate = endDate.minusDays(14).withHour(0).withMinute(0).withSecond(0);
//			log.info("No date filter provided, using default: last 14 days from {} to {}", startDate, endDate);
//		} else if (startDate == null) {
//			// Nếu chỉ có endDate, lấy 14 ngày trước endDate
//			startDate = endDate.minusDays(14).withHour(0).withMinute(0).withSecond(0);
//		} else if (endDate == null) {
//			// Nếu chỉ có startDate, lấy đến hiện tại
//			endDate = LocalDateTime.now();
//		}
//
//		log.info("Fetching revenue for shop {} from {} to {}", shopId, startDate, endDate);
//
//		List<ShopRevenue> revenues = shopRevenueRepository.findByShopIdAndDateRange(shopId, startDate, endDate);
//
//		log.info("Found {} revenue records", revenues.size());
//
//		// Group by date
//		Map<LocalDateTime, List<ShopRevenue>> groupedByDate = revenues.stream()
//				.collect(Collectors.groupingBy(sr -> sr.getRecordedAt().toLocalDate().atStartOfDay()));
//
//		List<ShopRevenueDTO> result = groupedByDate.entrySet().stream().map(entry -> {
//			ShopRevenueDTO dto = new ShopRevenueDTO();
//			dto.setDate(entry.getKey());
//			dto.setTotalOrders((long) entry.getValue().size());
//			dto.setOrderAmount(entry.getValue().stream().map(ShopRevenue::getOrderAmount).reduce(BigDecimal.ZERO,
//					BigDecimal::add));
//			dto.setCommissionAmount(entry.getValue().stream().map(ShopRevenue::getCommissionAmount)
//					.reduce(BigDecimal.ZERO, BigDecimal::add));
//			dto.setNetRevenue(
//					entry.getValue().stream().map(ShopRevenue::getNetRevenue).reduce(BigDecimal.ZERO, BigDecimal::add));
//			return dto;
//		}).sorted(Comparator.comparing(ShopRevenueDTO::getDate).reversed()).collect(Collectors.toList());
//
//		log.info("Grouped into {} days", result.size());
//
//		return result;
//	}
//
//	public List<CategoryRevenueDTO> getShopRevenueByCategory(Integer shopId, LocalDateTime startDate,
//			LocalDateTime endDate) {
//		log.info("Fetching category revenue for shopId: {}, startDate: {}, endDate: {}", shopId, startDate, endDate);
//
//		if (startDate == null) {
//			startDate = LocalDateTime.now().minusMonths(1);
//		}
//		if (endDate == null) {
//			endDate = LocalDateTime.now();
//		}
//		endDate = endDate.withHour(23).withMinute(59).withSecond(59);
//
//		String jpql = """
//				    SELECT new com.alotra.dto.shop.CategoryRevenueDTO(
//				        c.categoryName,
//				        CAST(SUM(od.subtotal) AS java.math.BigDecimal),
//				        CAST(SUM(od.subtotal) * (100.0 - COALESCE(s.commissionRate, 5.0)) / 100.0 AS java.math.BigDecimal),
//				        COUNT(DISTINCT o.orderID)
//				    )
//				    FROM Order o
//				    JOIN o.orderDetails od
//				    JOIN od.variant pv
//				    JOIN pv.product p
//				    JOIN p.category c
//				    JOIN o.shop s
//				    WHERE o.shop.shopId = :shopId
//				      AND o.orderStatus = 'Completed'
//				      AND o.completedAt >= :startDate
//				      AND o.completedAt <= :endDate
//				    GROUP BY c.categoryName, s.commissionRate
//				    ORDER BY SUM(od.subtotal) DESC
//				""";
//
//		try {
//			TypedQuery<CategoryRevenueDTO> query = entityManager.createQuery(jpql, CategoryRevenueDTO.class);
//			query.setParameter("shopId", shopId);
//			query.setParameter("startDate", startDate);
//			query.setParameter("endDate", endDate);
//
//			List<CategoryRevenueDTO> results = query.getResultList();
//			log.info("Found {} categories with revenue.", results.size());
//			return results;
//
//		} catch (Exception e) {
//			log.error("Error fetching category revenue: {}", e.getMessage(), e);
//			return new ArrayList<>(); // Return empty list on error
//		}
//	}
//
//	// ==================== APPROVAL STATUS ====================
//
//	public List<ApprovalResponseDTO> getPendingApprovals(Integer shopId, String entityTypeFilter,
//			String actionTypeFilter) {
//		List<ApprovalResponseDTO> allApprovals = new ArrayList<>();
//
//		// 1. Fetch ALL pending product approvals
//		List<ProductApproval> productApprovals = productApprovalRepository
//				.findByProduct_Shop_ShopIdAndStatusOrderByRequestedAtDesc(shopId, "Pending");
//
//		for (ProductApproval pa : productApprovals) {
//			ApprovalResponseDTO dto = new ApprovalResponseDTO();
//			dto.setApprovalId(pa.getApprovalId());
//			dto.setEntityType("PRODUCT"); // Set type explicitly
//			dto.setEntityId(pa.getProduct().getProductID());
//			dto.setActionType(pa.getActionType());
//			dto.setStatus(pa.getStatus());
//			dto.setChangeDetails(pa.getChangeDetails());
//			dto.setRequestedAt(pa.getRequestedAt());
//			dto.setRequestedByName(pa.getRequestedBy().getFullName());
//			Optional<Product> productOpt = productRepository.findById(pa.getProduct().getProductID());
//			productOpt.ifPresent(product -> dto.setEntityName(product.getProductName()));
//			allApprovals.add(dto);
//		}
//
//		// 2. Fetch ALL pending promotion approvals
//		List<PromotionApproval> promotionApprovals = promotionApprovalRepository
//				.findByPromotion_CreatedByShopID_ShopIdAndStatusOrderByRequestedAtDesc(shopId, "Pending");
//
//		for (PromotionApproval pa : promotionApprovals) {
//			ApprovalResponseDTO dto = new ApprovalResponseDTO();
//			dto.setApprovalId(pa.getApprovalId());
//			dto.setEntityType("PROMOTION"); // Set type explicitly
//			dto.setEntityId(pa.getPromotion().getPromotionId());
//			dto.setActionType(pa.getActionType());
//			dto.setStatus(pa.getStatus());
//			dto.setChangeDetails(pa.getChangeDetails());
//			dto.setRequestedAt(pa.getRequestedAt());
//			dto.setRequestedByName(pa.getRequestedBy().getFullName());
//			Optional<Promotion> promotionOpt = promotionRepository.findById(pa.getPromotion().getPromotionId());
//			promotionOpt.ifPresent(promo -> dto.setEntityName(promo.getPromotionName()));
//			allApprovals.add(dto);
//		}
//
//		// 3. Fetch ALL pending topping approvals
//		List<ToppingApproval> toppingApprovals = toppingApprovalRepository
//				.findByShop_ShopIdAndStatusOrderByRequestedAtDesc(shopId, "Pending");
//
//		for (ToppingApproval pa : toppingApprovals) {
//			ApprovalResponseDTO dto = new ApprovalResponseDTO();
//			dto.setApprovalId(pa.getApprovalId());
//			dto.setEntityType("TOPPING"); // Set type
//			dto.setEntityId(pa.getTopping() != null ? pa.getTopping().getToppingID() : null);
//			dto.setActionType(pa.getActionType());
//			dto.setStatus(pa.getStatus());
//			dto.setChangeDetails(pa.getChangeDetails());
//			dto.setRequestedAt(pa.getRequestedAt());
//			dto.setRequestedByName(pa.getRequestedBy().getFullName());
//			if (pa.getTopping() != null) {
//				dto.setEntityName(pa.getTopping().getToppingName());
//			} else if ("CREATE".equals(pa.getActionType())) {
//				// Thử đọc tên từ JSON cho trường hợp CREATE
//				try {
//					ToppingRequestDTO trd = objectMapper.readValue(pa.getChangeDetails(), ToppingRequestDTO.class);
//					dto.setEntityName(trd.getToppingName() + " (Mới)");
//				} catch (Exception e) {
//					dto.setEntityName("(Topping mới)");
//				}
//			}
//			allApprovals.add(dto);
//		}
//
//		// 4. Filter the combined list using Streams
//		List<ApprovalResponseDTO> filteredApprovals = allApprovals.stream()
//				.filter(dto -> !StringUtils.hasText(entityTypeFilter)
//						|| dto.getEntityType().equalsIgnoreCase(entityTypeFilter)) // Filter by entity type if provided
//				.filter(dto -> !StringUtils.hasText(actionTypeFilter)
//						|| dto.getActionType().equalsIgnoreCase(actionTypeFilter)) // Filter by action type if provided
//				.sorted(Comparator.comparing(ApprovalResponseDTO::getRequestedAt).reversed()) // Sort AFTER filtering
//				.collect(Collectors.toList());
//
//		return filteredApprovals; // Return the filtered and sorted list
//	}
//
//	private void createInternalProductPromotion(Product product, Integer discountPercentage, Shop shop,
//			Integer userId) {
//		try {
//			User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//			// Tạo Promotion với PromotionType = "PRODUCT"
//			Promotion promotion = new Promotion();
//			promotion.setCreatedByUserID(user);
//			promotion.setCreatedByShopID(shop);
//			promotion.setPromotionName("Giảm giá sản phẩm: " + product.getProductName());
//			promotion.setDescription("Khuyến mãi nội bộ cho sản phẩm");
//			promotion.setPromoCode("PRODUCT_" + product.getProductID() + "_" + System.currentTimeMillis());
//			promotion.setPromotionType("PRODUCT"); // *** QUAN TRỌNG ***
//			promotion.setDiscountType(null); // Không dùng cho PRODUCT type
//			promotion.setDiscountValue(null);
//			promotion.setStartDate(LocalDateTime.now());
//			promotion.setEndDate(LocalDateTime.now().plusYears(10)); // Vô thời hạn
//			promotion.setMinOrderValue(BigDecimal.ZERO);
//			promotion.setUsageLimit(0); // Không giới hạn
//			promotion.setStatus((byte) 1); // Active ngay
//			promotion.setCreatedAt(LocalDateTime.now());
//
//			promotion = promotionRepository.save(promotion);
//
//			// Tạo liên kết trong PromotionProduct
//			PromotionProduct pp = new PromotionProduct();
//			pp.setId(new PromotionProductId(promotion.getPromotionId(), product.getProductID()));
//			pp.setPromotion(promotion);
//			pp.setProduct(product);
//			pp.setDiscountPercentage(discountPercentage);
//
//			promotionProductRepository.save(pp);
//
//			log.info("Created internal product promotion ID: {} for product ID: {} with {}% discount",
//					promotion.getPromotionId(), product.getProductID(), discountPercentage);
//
//		} catch (Exception e) {
//			log.error("Error creating internal product promotion: {}", e.getMessage(), e);
//			// Không throw exception để không làm fail toàn bộ flow
//		}
//	}
//
//	// ==================== SHOP PROFILE MANAGEMENT ====================
//	// Thêm vào VendorService.java
//
//	@Transactional(readOnly = true)
//	public ShopProfileDTO getShopProfile(Integer shopId) {
//		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
//
//		ShopProfileDTO dto = new ShopProfileDTO();
//		dto.setShopId(shop.getShopId());
//		dto.setShopName(shop.getShopName());
//		dto.setDescription(shop.getDescription());
//		dto.setLogoURL(shop.getLogoURL());
//		dto.setCoverImageURL(shop.getCoverImageURL());
//		dto.setAddress(shop.getAddress());
//		dto.setPhoneNumber(shop.getPhoneNumber());
//		dto.setStatus(shop.getStatus());
//
//		// Convert status to text
//		switch (shop.getStatus()) {
//		case 0:
//			dto.setStatusText("Đang chờ duyệt");
//			break;
//		case 1:
//			dto.setStatusText("Đang hoạt động");
//			break;
//		case 2:
//			dto.setStatusText("Đã bị đình chỉ");
//			break;
//		default:
//			dto.setStatusText("Không xác định");
//		}
//
//		dto.setCommissionRate(shop.getCommissionRate());
//		dto.setCreatedAt(shop.getCreatedAt());
//		dto.setUpdatedAt(shop.getUpdatedAt());
//
//		// Owner info
//		if (shop.getUser() != null) {
//			dto.setOwnerName(shop.getUser().getFullName());
//			dto.setOwnerEmail(shop.getUser().getEmail());
//		}
//
//		return dto;
//	}
//
//	@Transactional
//	public void updateShopProfile(Integer shopId, ShopProfileDTO request, Integer userId) throws Exception {
//
//		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
//
//		// Kiểm tra quyền sở hữu
//		if (!shop.getUser().getId().equals(userId)) {
//			throw new RuntimeException("Unauthorized: You are not the owner of this shop");
//		}
//
//		// Kiểm tra trùng tên shop (nếu đổi tên)
//		if (!shop.getShopName().equals(request.getShopName())) {
//			Optional<Shop> existingShop = shopRepository.findByShopName(request.getShopName());
//			if (existingShop.isPresent() && !existingShop.get().getShopId().equals(shopId)) {
//				throw new RuntimeException("Tên cửa hàng đã tồn tại");
//			}
//		}
//
//		// Upload logo mới (nếu có)
//		if (request.getLogoFile() != null && !request.getLogoFile().isEmpty()) {
//			try {
//				Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(request.getLogoFile(),
//						"shops/logos", userId);
//				String newLogoUrl = uploadResult.get("secure_url");
//				if (newLogoUrl != null) {
//					shop.setLogoURL(newLogoUrl);
//					log.info("Uploaded new logo for shop {}: {}", shopId, newLogoUrl);
//				}
//			} catch (Exception e) {
//				log.error("Error uploading logo: {}", e.getMessage(), e);
//				throw new RuntimeException("Lỗi khi upload logo: " + e.getMessage());
//			}
//		}
//
//		// Upload cover image mới (nếu có)
//		if (request.getCoverImageFile() != null && !request.getCoverImageFile().isEmpty()) {
//			try {
//				Map<String, String> uploadResult = cloudinaryService
//						.uploadImageAndReturnDetails(request.getCoverImageFile(), "shops/covers", userId);
//				String newCoverUrl = uploadResult.get("secure_url");
//				if (newCoverUrl != null) {
//					shop.setCoverImageURL(newCoverUrl);
//					log.info("Uploaded new cover image for shop {}: {}", shopId, newCoverUrl);
//				}
//			} catch (Exception e) {
//				log.error("Error uploading cover image: {}", e.getMessage(), e);
//				throw new RuntimeException("Lỗi khi upload ảnh bìa: " + e.getMessage());
//			}
//		}
//
//		// Cập nhật thông tin cơ bản
//		shop.setShopName(request.getShopName());
//		shop.setDescription(request.getDescription());
//		shop.setAddress(request.getAddress());
//		shop.setPhoneNumber(request.getPhoneNumber());
//
//		// updatedAt sẽ tự động cập nhật qua @PreUpdate
//
//		shopRepository.save(shop);
//
//		log.info("Shop profile updated successfully - Shop ID: {}, User ID: {}", shopId, userId);
//	}
//	
//	// ==================== STAFF MANAGEMENT ====================
//	// Thêm dependencies vào constructor
//	private final ShopEmployeeRepository shopEmployeeRepository;
//	private final RoleRepository roleRepository;
//
//	@Transactional(readOnly = true)
//	public Page<ShopEmployeeDTO> getShopEmployees(Integer shopId, String status, String search, Pageable pageable) {
//	    
//	    Page<ShopEmployee> employees = shopEmployeeRepository.findShopEmployeesFiltered(
//	            shopId, status, search, pageable);
//	    
//	    return employees.map(employee -> {
//	        ShopEmployeeDTO dto = new ShopEmployeeDTO();
//	        dto.setEmployeeId(employee.getEmployeeId());
//	        dto.setUserId(employee.getUser().getId());
//	        dto.setFullName(employee.getUser().getFullName());
//	        dto.setEmail(employee.getUser().getEmail());
//	        dto.setPhoneNumber(employee.getUser().getPhoneNumber());
//	        dto.setAvatarURL(employee.getUser().getAvatarURL());
//	        dto.setStatus(employee.getStatus());
//	        dto.setAssignedAt(employee.getAssignedAt());
//	        dto.setUpdatedAt(employee.getUpdatedAt());
//	        
//	        // Lấy role name (STAFF hoặc SHIPPER)
//	        employee.getUser().getRoles().stream()
//	            .filter(role -> "STAFF".equals(role.getRoleName()) || "SHIPPER".equals(role.getRoleName()))
//	            .findFirst()
//	            .ifPresent(role -> {
//	                dto.setRoleId(role.getId());
//	                dto.setRoleName(role.getRoleName());
//	            });
//	        
//	        return dto;
//	    });
//	}
//
//	@Transactional(readOnly = true)
//	public User searchUserForEmployee(String searchTerm) {
//	    // Tìm user theo email hoặc phone
//	    Optional<User> userOpt;
//	    
//	    if (searchTerm.contains("@")) {
//	        userOpt = userRepository.findByEmail(searchTerm);
//	    } else {
//	        userOpt = userRepository.findByPhoneNumber(searchTerm);
//	    }
//	    
//	    if (!userOpt.isPresent()) {
//	        throw new RuntimeException("Không tìm thấy người dùng với thông tin: " + searchTerm);
//	    }
//	    
//	    User user = userOpt.get();
//	    
//	    // Kiểm tra user phải có status = 1 (Active)
//	    if (user.getStatus() != 1) {
//	        throw new RuntimeException("Tài khoản này chưa được kích hoạt hoặc đã bị khóa");
//	    }
//	    
//	    // Kiểm tra user phải có role CUSTOMER
//	    boolean isCustomer = user.getRoles().stream()
//	            .anyMatch(role -> "CUSTOMER".equals(role.getRoleName()));
//	    
//	    if (!isCustomer) {
//	        throw new RuntimeException("Người dùng này không phải là khách hàng hoặc đã có vai trò khác");
//	    }
//	    
//	    return user;
//	}
//
//	@Transactional
//	public void addEmployee(Integer shopId, Integer userId, Integer roleId) {
//	    
//	    Shop shop = shopRepository.findById(shopId)
//	            .orElseThrow(() -> new RuntimeException("Shop not found"));
//	    
//	    User user = userRepository.findById(userId)
//	            .orElseThrow(() -> new RuntimeException("User not found"));
//	    
//	    // Kiểm tra user đã là employee của shop này chưa
//	    if (shopEmployeeRepository.existsByShop_ShopIdAndUser_Id(shopId, userId)) {
//	        throw new RuntimeException("Người dùng này đã là nhân viên của cửa hàng");
//	    }
//	    
//	    // Kiểm tra roleId hợp lệ (4: SHIPPER, 5: STAFF)
//	    if (roleId != 4 && roleId != 5) {
//	        throw new RuntimeException("Vai trò không hợp lệ");
//	    }
//	    
//	    Role newRole = roleRepository.findById(roleId)
//	            .orElseThrow(() -> new RuntimeException("Role not found"));
//	    
//	    // Xóa role CUSTOMER và thêm role mới
//	    user.getRoles().removeIf(role -> "CUSTOMER".equals(role.getRoleName()));
//	    user.getRoles().add(newRole);
//	    userRepository.save(user);
//	    
//	    // Tạo ShopEmployee record
//	    ShopEmployee employee = new ShopEmployee();
//	    employee.setShop(shop);
//	    employee.setUser(user);
//	    employee.setStatus("Active");
//	    
//	    shopEmployeeRepository.save(employee);
//	    
//	    log.info("Added employee: User {} to Shop {} with role {}", userId, shopId, newRole.getRoleName());
//	}
//
//	@Transactional(readOnly = true)
//	public ShopEmployeeDTO getEmployeeDetail(Integer shopId, Integer employeeId) {
//	    
//	    ShopEmployee employee = shopEmployeeRepository.findById(employeeId)
//	            .orElseThrow(() -> new RuntimeException("Employee not found"));
//	    
//	    if (!employee.getShop().getShopId().equals(shopId)) {
//	        throw new RuntimeException("Unauthorized: Employee does not belong to this shop");
//	    }
//	    
//	    ShopEmployeeDTO dto = new ShopEmployeeDTO();
//	    dto.setEmployeeId(employee.getEmployeeId());
//	    dto.setUserId(employee.getUser().getId());
//	    dto.setFullName(employee.getUser().getFullName());
//	    dto.setEmail(employee.getUser().getEmail());
//	    dto.setPhoneNumber(employee.getUser().getPhoneNumber());
//	    dto.setAvatarURL(employee.getUser().getAvatarURL());
//	    dto.setStatus(employee.getStatus());
//	    dto.setAssignedAt(employee.getAssignedAt());
//	    dto.setUpdatedAt(employee.getUpdatedAt());
//	    
//	    // Lấy role hiện tại
//	    employee.getUser().getRoles().stream()
//	        .filter(role -> "STAFF".equals(role.getRoleName()) || "SHIPPER".equals(role.getRoleName()))
//	        .findFirst()
//	        .ifPresent(role -> {
//	            dto.setRoleId(role.getId());
//	            dto.setRoleName(role.getRoleName());
//	        });
//	    
//	    return dto;
//	}
//
//	@Transactional
//	public void updateEmployee(Integer shopId, Integer employeeId, Integer newRoleId, String newStatus) {
//	    
//	    ShopEmployee employee = shopEmployeeRepository.findById(employeeId)
//	            .orElseThrow(() -> new RuntimeException("Employee not found"));
//	    
//	    if (!employee.getShop().getShopId().equals(shopId)) {
//	        throw new RuntimeException("Unauthorized: Employee does not belong to this shop");
//	    }
//	    
//	    User user = employee.getUser();
//	    
//	    // Cập nhật role nếu thay đổi
//	    if (newRoleId != null && (newRoleId == 4 || newRoleId == 5)) {
//	        Role currentRole = user.getRoles().stream()
//	            .filter(role -> "STAFF".equals(role.getRoleName()) || "SHIPPER".equals(role.getRoleName()))
//	            .findFirst()
//	            .orElse(null);
//	        
//	        if (currentRole == null || !currentRole.getId().equals(newRoleId)) {
//	            // Xóa role cũ
//	            user.getRoles().removeIf(role -> "STAFF".equals(role.getRoleName()) || "SHIPPER".equals(role.getRoleName()));
//	            
//	            // Thêm role mới
//	            Role newRole = roleRepository.findById(newRoleId)
//	                    .orElseThrow(() -> new RuntimeException("Role not found"));
//	            user.getRoles().add(newRole);
//	            userRepository.save(user);
//	            
//	            log.info("Updated employee role: User {} to {}", user.getId(), newRole.getRoleName());
//	        }
//	    }
//	    
//	    // Cập nhật status
//	    if (newStatus != null && ("Active".equals(newStatus) || "Inactive".equals(newStatus))) {
//	        employee.setStatus(newStatus);
//	        shopEmployeeRepository.save(employee);
//	        
//	        log.info("Updated employee status: Employee {} to {}", employeeId, newStatus);
//	    }
//	}
//
//	@Transactional
//	public void deactivateEmployee(Integer shopId, Integer employeeId) {
//	    
//	    ShopEmployee employee = shopEmployeeRepository.findById(employeeId)
//	            .orElseThrow(() -> new RuntimeException("Employee not found"));
//	    
//	    if (!employee.getShop().getShopId().equals(shopId)) {
//	        throw new RuntimeException("Unauthorized: Employee does not belong to this shop");
//	    }
//	    
//	    // Chuyển status thành Inactive
//	    employee.setStatus("Inactive");
//	    shopEmployeeRepository.save(employee);
//	    
//	    // Optional: Có thể chuyển user về role CUSTOMER
//	    User user = employee.getUser();
//	    user.getRoles().removeIf(role -> "STAFF".equals(role.getRoleName()) || "SHIPPER".equals(role.getRoleName()));
//	    
//	    Role customerRole = roleRepository.findById(2) // CUSTOMER role ID = 2
//	            .orElseThrow(() -> new RuntimeException("Customer role not found"));
//	    user.getRoles().add(customerRole);
//	    userRepository.save(user);
//	    
//	    log.info("Deactivated employee: Employee {}, User {} reverted to CUSTOMER", employeeId, user.getId());
//	}
//}