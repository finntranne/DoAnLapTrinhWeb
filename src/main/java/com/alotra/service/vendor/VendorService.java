package com.alotra.service.vendor;

import com.alotra.dto.*;
import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.dto.product.ProductStatisticsDTO;
import com.alotra.dto.product.ProductVariantDTO;
import com.alotra.dto.product.SimpleCategoryDTO;
import com.alotra.dto.product.SimpleProductDTO;
import com.alotra.dto.product.SimpleSizeDTO;
import com.alotra.dto.promotion.PromotionStatisticsDTO;
import com.alotra.dto.promotion.ProductDiscountDTO;
import com.alotra.dto.promotion.PromotionRequestDTO;
import com.alotra.dto.response.ApprovalResponseDTO;
import com.alotra.dto.shop.CategoryRevenueDTO;
import com.alotra.dto.shop.ShopDashboardDTO;
import com.alotra.dto.shop.ShopOrderDTO;
import com.alotra.dto.shop.ShopRevenueDTO;
import com.alotra.dto.topping.ToppingRequestDTO;
import com.alotra.dto.topping.ToppingStatisticsDTO;
import com.alotra.entity.*;
import com.alotra.entity.order.Order;
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductApproval;
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Size;
import com.alotra.entity.product.Topping;
import com.alotra.entity.product.ToppingApproval;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.promotion.PromotionApproval;
import com.alotra.entity.promotion.PromotionProduct;
import com.alotra.entity.promotion.PromotionProductId;
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
import com.alotra.repository.product.ToppingApprovalRepository;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.repository.promotion.PromotionApprovalRepository;
import com.alotra.repository.promotion.PromotionProductRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.shop.ShopRevenueRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.cloudinary.CloudinaryService;
import com.alotra.service.notification.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
	private final PromotionProductRepository promotionProductRepository;
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
	private final ToppingRepository toppingRepository;
	private final ToppingApprovalRepository toppingApprovalRepository;

	private final ObjectMapper objectMapper;
	@PersistenceContext // Inject EntityManager for JPQL
	private EntityManager entityManager;

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
		Long pendingToppings = toppingApprovalRepository.countPendingByShopId(shopId);
		dashboard.setPendingApprovals((int) (pendingProducts + pendingPromotions + pendingToppings));

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

//	public Page<ProductStatisticsDTO> getShopProducts(Integer shopId, Byte status, Integer categoryId, String search,
//			Pageable pageable) {
//		// *** PASS categoryId TO REPOSITORY ***
//		Page<Product> products = productRepository.searchShopProducts(shopId, status, categoryId, search, pageable);
//
//		// The rest of the mapping logic remains the same
//		return products.map(product -> {
//			ProductStatisticsDTO dto = new ProductStatisticsDTO();
//			// ... (Gán các thuộc tính khác như cũ)
//			dto.setProductId(product.getProductID());
//			dto.setProductName(product.getProductName());
//			dto.setSoldCount(product.getSoldCount());
//			dto.setAverageRating(product.getAverageRating());
//			dto.setTotalReviews(product.getTotalReviews());
//			dto.setViewCount(product.getViewCount());
//			// Use DTO status directly
//			dto.setStatus(product.getStatus() == 1 ? "Đang hoạt động" : "Không hoạt động"); // Updated status text
//			product.getImages().stream().filter(ProductImage::getIsPrimary).findFirst()
//					.ifPresent(img -> dto.setPrimaryImageUrl(img.getImageURL()));
//			product.getVariants().stream().map(ProductVariant::getPrice).min(BigDecimal::compareTo)
//					.ifPresent(dto::setMinPrice);
//
//			Optional<ProductApproval> latestApprovalOpt = productApprovalRepository
//					.findTopByProduct_ProductIDOrderByRequestedAtDesc(product.getProductID());
//
//			if (latestApprovalOpt.isPresent()) {
//				ProductApproval latestApproval = latestApprovalOpt.get();
//				String currentStatus = latestApproval.getStatus();
//
//				if ("Pending".equals(currentStatus) || "Rejected".equals(currentStatus)) {
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
//					if ("Pending".equals(currentStatus)) {
//						dto.setApprovalStatus("Đang chờ: " + actionTypeText);
//					} else {
//						dto.setApprovalStatus("Bị từ chối: " + actionTypeText);
//					}
//				}
//			}
//			return dto;
//		});
//	}

	public Page<ProductStatisticsDTO> getShopProducts(Integer shopId, Byte status, Integer categoryId,
			String approvalStatus, String search, Pageable pageable) {

		String normalizedSearch = (search != null && search.trim().isEmpty()) ? null : search;
		String normalizedApprovalStatus = (approvalStatus != null && approvalStatus.trim().isEmpty()) ? null
				: approvalStatus;

		log.debug("Calling repository with: shopId={}, status={}, categoryId={}, approvalStatus='{}', search='{}'",
				shopId, status, categoryId, normalizedApprovalStatus, normalizedSearch);

		// Gọi repository với parameters đã chuẩn hóa
		Page<Product> products = productRepository.searchShopProducts(shopId, status, categoryId,
				normalizedApprovalStatus, normalizedSearch, pageable);

		// Mapping logic giữ nguyên
		return products.map(product -> {
			ProductStatisticsDTO dto = new ProductStatisticsDTO();
			dto.setProductId(product.getProductID());
			dto.setProductName(product.getProductName());
			dto.setSoldCount(product.getSoldCount());
			dto.setAverageRating(product.getAverageRating());
			dto.setTotalReviews(product.getTotalReviews());
			dto.setViewCount(product.getViewCount());
			dto.setStatus(product.getStatus() == 1 ? "Đang hoạt động" : "Không hoạt động");

			// Safely get primary image URL
			product.getImages().stream().filter(img -> img != null && Boolean.TRUE.equals(img.getIsPrimary()))
					.findFirst().ifPresent(img -> dto.setPrimaryImageUrl(img.getImageURL()));

			// Safely get min price
			product.getVariants().stream().filter(Objects::nonNull).map(ProductVariant::getPrice)
					.filter(Objects::nonNull).min(BigDecimal::compareTo).ifPresent(dto::setMinPrice);

			// Fetch approval status for DISPLAY
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

	public void requestProductCreation(Integer shopId, ProductRequestDTO request, Integer userId,
			Set<Topping> selectedToppings) throws JsonProcessingException { // Thêm throws Exception nếu
																			// uploadImageAndReturnDetails có thể ném
																			// lỗi I/O

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

		product.setAvailableToppings(selectedToppings);

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

//	public void requestProductUpdate(Integer shopId, ProductRequestDTO request, Integer userId, Set<Topping> selectedToppings)
//			throws JsonProcessingException { // Add potential Exception from upload
//
//		Product product = productRepository.findById(request.getProductId())
//				.orElseThrow(() -> new RuntimeException("Product not found"));
//
//		if (request.getImages() != null && !request.getImages().isEmpty() && request.getImages().stream().anyMatch(f -> f != null && !f.isEmpty())) {
//            log.info("Processing new images for product update ID: {}", request.getProductId());
//            List<String> uploadedImageUrls = new ArrayList<>();
//            for (MultipartFile file : request.getImages()) {
//                if (file != null && !file.isEmpty()) {
//                    try {
//                        Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(file, "products", userId);
//                        uploadedImageUrls.add(uploadResult.get("secure_url"));
//                    } catch (Exception e) {
//                        throw new RuntimeException("Lỗi khi upload hình ảnh mới: " + e.getMessage());
//                    }
//                }
//            }
//            if (!uploadedImageUrls.isEmpty()) {
//                request.setNewImageUrls(uploadedImageUrls); // DTO này cần trường newImageUrls
//            }
//       } else {
//           request.setNewImageUrls(null);
//       }
//		
//		if (!product.getShop().getShopId().equals(shopId)) {
//			throw new RuntimeException("Unauthorized: Product does not belong to this shop");
//		}
//
//		List<ProductApproval> existingApprovals = productApprovalRepository
//				.findByProduct_ProductIDAndStatus(product.getProductID(), "Pending");
//
//		if (!existingApprovals.isEmpty()) {
//			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho sản phẩm này");
//		}
//
//		// *** START IMAGE UPDATE HANDLING ***
//		List<String> uploadedImageUrls = new ArrayList<>();
//		// List<String> uploadedImagePublicIds = new ArrayList<>(); // Optional
//
//		// Check if new images were actually submitted
//		boolean newImagesSubmitted = request.getImages() != null && !request.getImages().isEmpty()
//				&& request.getImages().stream().anyMatch(f -> f != null && !f.isEmpty());
//
//		if (newImagesSubmitted) {
//			log.info("Processing new images for product update ID: {}", request.getProductId());
//			for (MultipartFile file : request.getImages()) {
//				if (file != null && !file.isEmpty()) {
//					try {
//						// Assuming uploadImage returns Map<String, String>
//						// Adjust if your service returns something else (e.g., just the URL)
//						Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(file,
//								"products", userId); // Use a
//						// method
//						// returning
//						// details
//						String imageUrl = uploadResult.get("secure_url");
//						// String publicId = uploadResult.get("public_id"); // Optional
//
//						if (imageUrl != null) {
//							uploadedImageUrls.add(imageUrl);
//							// uploadedImagePublicIds.add(publicId); // Optional
//							log.info("Uploaded new image: {}", imageUrl);
//						} else {
//							log.warn("Cloudinary upload did not return a URL for one of the files.");
//						}
//
//					} catch (Exception e) {
//						log.error("Error uploading a new image during product update: {}", e.getMessage(), e);
//						// Decide: Throw exception to stop, or just log and continue without the failed
//						// image?
//						// Throwing is safer to ensure consistency.
//						throw new RuntimeException("Lỗi khi upload hình ảnh mới: " + e.getMessage());
//					}
//				} else {
//					// Handle potential null/empty entries if the list allows them
//					// Or ensure the list passed from the controller is clean
//				}
//			}
//			// Populate the DTO fields ONLY IF new images were processed
//			if (!uploadedImageUrls.isEmpty()) {
//				request.setNewImageUrls(uploadedImageUrls);
//				// request.setNewImagePublicIds(uploadedImagePublicIds); // Optional
//			} else {
//				// If upload resulted in no URLs (e.g., all failed, or empty files submitted)
//				// Ensure the fields are null or empty list so JSON doesn't contain them
//				// accidentally
//				request.setNewImageUrls(null);
//				// request.setNewImagePublicIds(null);
//				log.warn("New images were submitted, but none were successfully uploaded or returned URLs.");
//			}
//		} else {
//			// No new image files submitted, ensure fields are null/empty
//			request.setNewImageUrls(null);
//			// request.setNewImagePublicIds(null);
//			log.info("No new image files submitted for product update ID: {}", request.getProductId());
//		}
//		// *** END IMAGE UPDATE HANDLING ***
//
//		// Create approval request - NOW the DTO contains new image info (if any)
//		ProductApproval approval = new ProductApproval();
//		approval.setProduct(product);
//		approval.setActionType("UPDATE");
//		approval.setStatus("Pending");
//		approval.setChangeDetails(objectMapper.writeValueAsString(request)); // Serialize the UPDATED DTO
//		approval.setRequestedBy(
//				userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
//		approval.setRequestedAt(LocalDateTime.now());
//
//		approval = productApprovalRepository.save(approval);
//
//		log.info("Product approval created with ID: {}", approval.getApprovalId());
//
//		try {
//			notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
//		} catch (Exception e) {
//			log.error("Error sending notification: {}", e.getMessage());
//		}
//
//		log.info("Product update requested - Product ID: {}, Shop ID: {}, Approval ID: {}", product.getProductID(),
//				shopId, approval.getApprovalId());
//	}

	public void requestProductUpdate(Integer shopId, ProductRequestDTO request, Integer userId,
			Set<Topping> selectedToppings) throws Exception { // Sửa thành Exception

		Product product = getProductDetail(shopId, request.getProductId()); // Kiểm tra quyền
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		List<ProductApproval> existingApprovals = productApprovalRepository
				.findByProduct_ProductIDAndStatus(product.getProductID(), "Pending");

		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho sản phẩm này");
		}

		// *** BẮT ĐẦU SỬA LOGIC UPLOAD ẢNH ***
		// (Xóa khối logic upload ảnh trùng lặp ở trên)
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
				request.setNewImageUrls(uploadedImageUrls); // Gán URL mới vào DTO
			}
		} else {
			request.setNewImageUrls(null); // Không có file mới
			log.info("No new image files submitted for product update ID: {}", request.getProductId());
		}
		// *** KẾT THÚC SỬA LOGIC UPLOAD ẢNH ***

		// Tạo yêu cầu phê duyệt
		ProductApproval approval = new ProductApproval();
		approval.setProduct(product);
		approval.setActionType("UPDATE");
		approval.setStatus("Pending");

		// DTO (request) bây giờ đã chứa:
		// 1. availableToppingIds (từ controller)
		// 2. promotionIds (từ controller)
		// 3. newImageUrls (từ logic upload)
		approval.setChangeDetails(objectMapper.writeValueAsString(request)); // Serialize DTO

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

		if (product.getAvailableToppings() != null) {
			Set<Integer> toppingIds = product.getAvailableToppings().stream().map(Topping::getToppingID)
					.collect(Collectors.toSet());
			dto.setAvailableToppingIds(toppingIds);
		}

		if (product.getPromotionProducts() != null) {
			Set<Integer> promoIds = product.getPromotionProducts().stream()
					.map(pp -> pp.getPromotion().getPromotionId()).collect(Collectors.toSet());
			dto.setPromotionIds(promoIds);
		}

		return dto;
	}

	// ==================== TOPPING MANAGEMENT ====================

	public Page<ToppingStatisticsDTO> getShopToppings(Integer shopId, Byte status, String search, Pageable pageable) {
		Page<Topping> toppings = toppingRepository.findShopToppingsFiltered(shopId, status, search, pageable);

		return toppings.map(topping -> {
			String approvalStatus = null;
			String activityStatus;

			Optional<ToppingApproval> latestApprovalOpt = toppingApprovalRepository
					.findTopByTopping_ToppingIDOrderByRequestedAtDesc(topping.getToppingID());

			if (latestApprovalOpt.isPresent()) {
				ToppingApproval latestApproval = latestApprovalOpt.get();
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
					approvalStatus = ("Pending".equals(currentDbStatus) ? "Đang chờ: " : "Bị từ chối: ")
							+ actionTypeText;
				}
			}

			activityStatus = (topping.getStatus() == 1) ? "Đang hoạt động" : "Không hoạt động";

			return new ToppingStatisticsDTO(topping, approvalStatus, activityStatus);
		});
	}

	public void requestToppingCreation(Integer shopId, ToppingRequestDTO request, Integer userId) throws Exception { // Ném
																														// Exception
		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		String uploadedImageUrl = null;
		// *** THÊM LOGIC UPLOAD ẢNH ***
		if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
			try {
				// Upload lên Cloudinary vào thư mục "toppings"
				Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(request.getImageFile(),
						"toppings", userId);
				uploadedImageUrl = uploadResult.get("secure_url");
			} catch (Exception e) {
				log.error("Lỗi upload ảnh topping: {}", e.getMessage(), e);
				throw new RuntimeException("Lỗi khi upload hình ảnh topping: " + e.getMessage());
			}
		}
		// *** KẾT THÚC LOGIC UPLOAD ẢNH ***

		// Tạo Topping mới với Status = 0 (Inactive)
		Topping topping = new Topping();
		topping.setShop(shop);
		topping.setToppingName(request.getToppingName());
		topping.setAdditionalPrice(request.getAdditionalPrice());
		topping.setImageURL(uploadedImageUrl); // *** SỬA: Gán URL đã upload ***
		topping.setStatus((byte) 0); // Chờ duyệt

		topping = toppingRepository.save(topping); // Lưu topping để lấy ID

		// Tạo yêu cầu phê duyệt
		ToppingApproval approval = new ToppingApproval();
		approval.setTopping(topping);
		approval.setShop(shop);
		approval.setActionType("CREATE");
		approval.setStatus("Pending");

		request.setImageURL(uploadedImageUrl); // *** THÊM: Gán URL vào DTO trước khi lưu JSON ***
		approval.setChangeDetails(objectMapper.writeValueAsString(request));

		approval.setRequestedBy(user);

		toppingApprovalRepository.save(approval);

		notificationService.notifyAdminsAboutNewApproval("TOPPING", topping.getToppingID());
	}

	public Topping getToppingDetail(Integer shopId, Integer toppingId) {
		Topping topping = toppingRepository.findById(toppingId)
				.orElseThrow(() -> new RuntimeException("Topping not found"));
		if (!topping.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Topping does not belong to this shop");
		}
		return topping;
	}

	public ToppingRequestDTO convertToppingToDTO(Topping topping) {
		ToppingRequestDTO dto = new ToppingRequestDTO();
		dto.setToppingId(topping.getToppingID());
		dto.setToppingName(topping.getToppingName());
		dto.setAdditionalPrice(topping.getAdditionalPrice());
		dto.setImageURL(topping.getImageURL());
		return dto;
	}

	public void requestToppingUpdate(Integer shopId, ToppingRequestDTO request, Integer userId) throws Exception { // Ném
																													// Exception
		Topping topping = getToppingDetail(shopId, request.getToppingId()); // Kiểm tra quyền sở hữu
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		List<ToppingApproval> existingApprovals = toppingApprovalRepository
				.findByTopping_ToppingIDAndStatus(topping.getToppingID(), "Pending");
		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho topping này");
		}

		// *** THÊM LOGIC UPLOAD ẢNH (CHO UPDATE) ***
		if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
			// Nếu có file mới, upload và set URL mới cho DTO
			try {
				Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(request.getImageFile(),
						"toppings", userId);
				request.setImageURL(uploadResult.get("secure_url")); // Gán URL mới vào DTO
			} catch (Exception e) {
				log.error("Lỗi upload ảnh topping (update): {}", e.getMessage(), e);
				throw new RuntimeException("Lỗi khi upload hình ảnh topping: " + e.getMessage());
			}
		} else {
			// Nếu không có file mới, giữ lại URL ảnh cũ từ database
			request.setImageURL(topping.getImageURL());
		}
		// *** KẾT THÚC LOGIC UPLOAD ẢNH ***

		ToppingApproval approval = new ToppingApproval();
		approval.setTopping(topping);
		approval.setShop(topping.getShop());
		approval.setActionType("UPDATE");
		approval.setStatus("Pending");
		approval.setChangeDetails(objectMapper.writeValueAsString(request)); // Lưu thay đổi (đã bao gồm imageURL)
		approval.setRequestedBy(user);

		toppingApprovalRepository.save(approval);
		notificationService.notifyAdminsAboutNewApproval("TOPPING", topping.getToppingID());
	}

	public void requestToppingDeletion(Integer shopId, Integer toppingId, Integer userId) {
		Topping topping = getToppingDetail(shopId, toppingId); // Kiểm tra quyền sở hữu
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		List<ToppingApproval> existingApprovals = toppingApprovalRepository
				.findByTopping_ToppingIDAndStatus(topping.getToppingID(), "Pending");
		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho topping này");
		}

		ToppingApproval approval = new ToppingApproval();
		approval.setTopping(topping);
		approval.setShop(topping.getShop());
		approval.setActionType("DELETE");
		approval.setStatus("Pending");
		approval.setRequestedBy(user);

		toppingApprovalRepository.save(approval);
		// (Tùy chọn) Gửi thông báo
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

//	public void requestPromotionCreation(Integer shopId, PromotionRequestDTO request, Integer userId)
//			throws JsonProcessingException {
//
//		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
//
//		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//		// Tạo promotion mới với status = 0 (Inactive)
//		Promotion promotion = new Promotion();
//		promotion.setCreatedByUserID(user);
//		promotion.setCreatedByShopID(shop);
//		promotion.setPromotionName(request.getPromotionName());
//		promotion.setDescription(request.getDescription());
//		promotion.setPromoCode(request.getPromoCode());
//		promotion.setDiscountType(request.getDiscountType());
//		promotion.setDiscountValue(request.getDiscountValue());
//		promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
//		promotion.setStartDate(request.getStartDate());
//		promotion.setEndDate(request.getEndDate());
//		promotion.setMinOrderValue(request.getMinOrderValue());
//		promotion.setUsageLimit(request.getUsageLimit());
//		promotion.setUsedCount(0);
//		promotion.setStatus((byte) 0); // Inactive until approved
//		promotion.setCreatedAt(LocalDateTime.now());
//
//		promotion = promotionRepository.save(promotion);
//
//		// Tạo yêu cầu phê duyệt
//		PromotionApproval approval = new PromotionApproval();
//		approval.setPromotion(promotion);
//		approval.setActionType("CREATE");
//		approval.setStatus("Pending");
//		approval.setChangeDetails(objectMapper.writeValueAsString(request));
//		approval.setRequestedBy(user);
//		approval.setRequestedAt(LocalDateTime.now());
//
//		promotionApprovalRepository.save(approval);
//
//		notificationService.notifyAdminsAboutNewApproval("PROMOTION", promotion.getPromotionId());
//
//		log.info("Promotion creation requested - Promotion ID: {}, Shop ID: {}", promotion.getPromotionId(), shopId);
//	}
//	

	public void requestPromotionCreation(Integer shopId, PromotionRequestDTO request, Integer userId)
			throws JsonProcessingException {

		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		// Kiểm tra logic ngày tháng (có thể làm ở DTO)
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
		promotion.setPromotionType(request.getPromotionType());
		promotion.setStatus((byte) 0); // Luôn là 0 (Pending) khi tạo
		// (createdAt được @PrePersist xử lý)

		// Tách logic dựa trên loại khuyến mãi
		if ("ORDER".equals(request.getPromotionType())) {
			// --- LOGIC CHO KHUYẾN MÃI TOÀN ĐƠN ---
			if (request.getDiscountType() == null || request.getDiscountValue() == null) {
				throw new IllegalArgumentException(
						"Loại giảm giá và Giá trị giảm giá là bắt buộc cho khuyến mãi toàn đơn.");
			}
			promotion.setDiscountType(request.getDiscountType());
			promotion.setDiscountValue(request.getDiscountValue());
			promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
			promotion.setMinOrderValue(request.getMinOrderValue());
		} else if ("PRODUCT".equals(request.getPromotionType())) {
			// --- LOGIC CHO KHUYẾN MÃI SẢN PHẨM ---
			if (request.getProductDiscounts() == null || request.getProductDiscounts().isEmpty()) {
				throw new IllegalArgumentException("Cần chọn ít nhất một sản phẩm để áp dụng khuyến mãi.");
			}
			// Các trường discount... của Promotion sẽ là NULL (hoặc 0 nếu DB không cho
			// phép)
			promotion.setMinOrderValue(BigDecimal.ZERO);
		} else {
			throw new IllegalArgumentException("Loại khuyến mãi không hợp lệ.");
		}

		Promotion savedPromotion = promotionRepository.save(promotion); // Lưu Promotion

		// Nếu là loại SẢN PHẨM, lưu các sản phẩm liên kết
		if ("PRODUCT".equals(savedPromotion.getPromotionType()) && request.getProductDiscounts() != null) {
			List<PromotionProduct> promoProducts = new ArrayList<>();

			List<Integer> productIds = request.getProductDiscounts().stream().map(ProductDiscountDTO::getProductId)
					.collect(Collectors.toList());

			// Lấy các sản phẩm từ DB
			List<Product> products = productRepository.findAllById(productIds);

			// Đảm bảo tất cả sản phẩm đều thuộc shop này (bảo mật)
			boolean allOwned = products.stream().allMatch(p -> p.getShop().getShopId().equals(shopId));
			if (!allOwned || products.size() != request.getProductDiscounts().size()) {
				throw new RuntimeException("Phát hiện sản phẩm không hợp lệ hoặc không thuộc sở hữu của shop.");
			}

			Map<Integer, Product> productMap = products.stream()
					.collect(Collectors.toMap(Product::getProductID, p -> p));

			for (ProductDiscountDTO dto : request.getProductDiscounts()) {
				PromotionProduct pp = new PromotionProduct();
				pp.setId(new PromotionProductId(savedPromotion.getPromotionId(), dto.getProductId()));
				pp.setPromotion(savedPromotion);
				pp.setProduct(productMap.get(dto.getProductId()));
				pp.setDiscountPercentage(dto.getDiscountPercentage());
				promoProducts.add(pp);
			}
			promotionProductRepository.saveAll(promoProducts); // Lưu bảng nối
		}

		// Tạo yêu cầu phê duyệt
		PromotionApproval approval = new PromotionApproval();
		approval.setPromotion(savedPromotion);
		approval.setActionType("CREATE");
		approval.setStatus("Pending");
		approval.setChangeDetails(objectMapper.writeValueAsString(request)); // DTO giờ đã chứa cả productDiscounts
		approval.setRequestedBy(user);

		promotionApprovalRepository.save(approval);
		notificationService.notifyAdminsAboutNewApproval("PROMOTION", savedPromotion.getPromotionId());
	}
	
	public List<SimpleProductDTO> getShopProductsForSelection(Integer shopId) {
	    List<Product> products = productRepository.findActiveProductsByShop(shopId);
	    return products.stream()
	        .map(p -> new SimpleProductDTO(p.getProductID(), p.getProductName()))
	        .collect(Collectors.toList());
	}

//	public void requestPromotionUpdate(Integer shopId, PromotionRequestDTO request, Integer userId)
//			throws JsonProcessingException {
//
//		Promotion promotion = promotionRepository.findById(request.getPromotionId())
//				.orElseThrow(() -> new RuntimeException("Promotion not found"));
//
//		if (!promotion.getCreatedByShopID().getShopId().equals(shopId)) {
//			throw new RuntimeException("Unauthorized: Promotion does not belong to this shop");
//		}
//
//		// *** ĐÃ SỬA: Kiểm tra bất kỳ yêu cầu pending nào (giống Product) ***
//		List<PromotionApproval> existingApprovals = promotionApprovalRepository
//				.findByPromotion_PromotionIdAndStatus(promotion.getPromotionId(), "Pending");
//
//		if (!existingApprovals.isEmpty()) {
//			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho khuyến mãi này");
//		}
//
//		// Tạo yêu cầu phê duyệt
//		PromotionApproval approval = new PromotionApproval();
//		approval.setPromotion(promotion);
//		approval.setActionType("UPDATE");
//		approval.setStatus("Pending");
//		approval.setChangeDetails(objectMapper.writeValueAsString(request));
//		approval.setRequestedBy(
//				userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
//		approval.setRequestedAt(LocalDateTime.now());
//
//		promotionApprovalRepository.save(approval);
//
//		notificationService.notifyAdminsAboutNewApproval("PROMOTION", promotion.getPromotionId());
//
//		log.info("Promotion update requested - Promotion ID: {}, Shop ID: {}", promotion.getPromotionId(), shopId);
//	}

	@Transactional
	public void requestPromotionUpdate(Integer shopId, PromotionRequestDTO request, Integer userId) throws Exception { // Thêm
																														// throws
																														// Exception

		log.info("Requesting promotion update cho ID: {}", request.getPromotionId());

		// 1. Lấy khuyến mãi gốc và kiểm tra quyền sở hữu
		Promotion promotion = getPromotionDetail(shopId, request.getPromotionId());
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		// 2. Kiểm tra xem có yêu cầu nào đang chờ không
		List<PromotionApproval> existingApprovals = promotionApprovalRepository
				.findByPromotion_PromotionIdAndStatus(promotion.getPromotionId(), "Pending");
		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho khuyến mãi này");
		}

		// 3. Kiểm tra logic ngày tháng
		if (request.getStartDate().isAfter(request.getEndDate())) {
			throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
		}

		// 4. Xử lý logic nghiệp vụ cho 2 loại
		// (Lưu ý: Logic nghiệp vụ (xóa/thêm) sẽ KHÔNG chạy ngay,
		// mà sẽ được lưu vào JSON để Admin duyệt và Stored Procedure xử lý)

		if ("ORDER".equals(request.getPromotionType())) {
			if (request.getDiscountType() == null || request.getDiscountValue() == null) {
				throw new IllegalArgumentException(
						"Loại giảm giá và Giá trị giảm giá là bắt buộc cho khuyến mãi toàn đơn.");
			}
		} else if ("PRODUCT".equals(request.getPromotionType())) {
			if (request.getProductDiscounts() == null || request.getProductDiscounts().isEmpty()) {
				throw new IllegalArgumentException("Cần chọn ít nhất một sản phẩm để áp dụng khuyến mãi.");
			}
			// Kiểm tra bảo mật (đảm bảo sản phẩm thuộc shop)
			List<Integer> productIds = request.getProductDiscounts().stream().map(ProductDiscountDTO::getProductId)
					.collect(Collectors.toList());
			List<Product> products = productRepository.findAllById(productIds);
			boolean allOwned = products.stream().allMatch(p -> p.getShop().getShopId().equals(shopId));
			if (!allOwned || products.size() != request.getProductDiscounts().size()) {
				throw new RuntimeException("Phát hiện sản phẩm không hợp lệ hoặc không thuộc sở hữu của shop.");
			}
		} else {
			throw new IllegalArgumentException("Loại khuyến mãi không hợp lệ.");
		}

		// 5. Tạo yêu cầu phê duyệt
		PromotionApproval approval = new PromotionApproval();
		approval.setPromotion(promotion);
		approval.setActionType("UPDATE");
		approval.setStatus("Pending");
		// DTO (request) chứa tất cả thông tin mới (loại, giá trị, danh sách sản
		// phẩm...)
		approval.setChangeDetails(objectMapper.writeValueAsString(request));
		approval.setRequestedBy(user);
		approval.setRequestedAt(LocalDateTime.now()); // Gán thời gian yêu cầu

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
	public Page<ShopOrderDTO> getShopOrders(Integer shopId, String status, String searchQuery, Pageable pageable) {

		Page<Order> orders = orderRepository.findShopOrdersFiltered(shopId, status, searchQuery, pageable);

		// Mapping logic remains the same
		return orders.map(order -> {
			ShopOrderDTO dto = new ShopOrderDTO();
			dto.setOrderId(order.getOrderID());
			dto.setOrderDate(order.getOrderDate());
			dto.setOrderStatus(order.getOrderStatus());
			dto.setPaymentMethod(order.getPaymentMethod());
			dto.setPaymentStatus(order.getPaymentStatus());
			dto.setGrandTotal(order.getGrandTotal());
			// Use associated User entity for customer info
			if (order.getUser() != null) {
				dto.setCustomerName(order.getUser().getFullName());
				dto.setCustomerPhone(order.getUser().getPhoneNumber()); // Assuming User has phoneNumber
			} else {
				dto.setCustomerName("N/A"); // Handle case where user might be null?
				dto.setCustomerPhone("N/A");
			}
			dto.setRecipientName(order.getRecipientName());
			dto.setRecipientPhone(order.getRecipientPhone());
			dto.setShippingAddress(order.getShippingAddress());

			if (order.getShipper() != null) {
				dto.setShipperName(order.getShipper().getFullName());
			}

			// Calculate total items (safer way)
			dto.setTotalItems(order.getOrderDetails() != null ? order.getOrderDetails().size() : 0);

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
		// Nếu không có filter, mặc định lấy 14 ngày gần nhất
		if (startDate == null && endDate == null) {
			endDate = LocalDateTime.now();
			startDate = endDate.minusDays(14).withHour(0).withMinute(0).withSecond(0);
			log.info("No date filter provided, using default: last 14 days from {} to {}", startDate, endDate);
		} else if (startDate == null) {
			// Nếu chỉ có endDate, lấy 14 ngày trước endDate
			startDate = endDate.minusDays(14).withHour(0).withMinute(0).withSecond(0);
		} else if (endDate == null) {
			// Nếu chỉ có startDate, lấy đến hiện tại
			endDate = LocalDateTime.now();
		}

		log.info("Fetching revenue for shop {} from {} to {}", shopId, startDate, endDate);

		List<ShopRevenue> revenues = shopRevenueRepository.findByShopIdAndDateRange(shopId, startDate, endDate);

		log.info("Found {} revenue records", revenues.size());

		// Group by date
		Map<LocalDateTime, List<ShopRevenue>> groupedByDate = revenues.stream()
				.collect(Collectors.groupingBy(sr -> sr.getRecordedAt().toLocalDate().atStartOfDay()));

		List<ShopRevenueDTO> result = groupedByDate.entrySet().stream().map(entry -> {
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

		log.info("Grouped into {} days", result.size());

		return result;
	}

	public List<CategoryRevenueDTO> getShopRevenueByCategory(Integer shopId, LocalDateTime startDate,
			LocalDateTime endDate) {
		log.info("Fetching category revenue for shopId: {}, startDate: {}, endDate: {}", shopId, startDate, endDate);

		if (startDate == null) {
			startDate = LocalDateTime.now().minusMonths(1);
		}
		if (endDate == null) {
			endDate = LocalDateTime.now();
		}
		endDate = endDate.withHour(23).withMinute(59).withSecond(59);

		String jpql = """
				    SELECT new com.alotra.dto.shop.CategoryRevenueDTO(
				        c.categoryName,
				        CAST(SUM(od.subtotal) AS java.math.BigDecimal),
				        CAST(SUM(od.subtotal) * (100.0 - COALESCE(s.commissionRate, 5.0)) / 100.0 AS java.math.BigDecimal),
				        COUNT(DISTINCT o.orderID)
				    )
				    FROM Order o
				    JOIN o.orderDetails od
				    JOIN od.variant pv
				    JOIN pv.product p
				    JOIN p.category c
				    JOIN o.shop s
				    WHERE o.shop.shopId = :shopId
				      AND o.orderStatus = 'Completed'
				      AND o.completedAt >= :startDate
				      AND o.completedAt <= :endDate
				    GROUP BY c.categoryName, s.commissionRate
				    ORDER BY SUM(od.subtotal) DESC
				""";

		try {
			TypedQuery<CategoryRevenueDTO> query = entityManager.createQuery(jpql, CategoryRevenueDTO.class);
			query.setParameter("shopId", shopId);
			query.setParameter("startDate", startDate);
			query.setParameter("endDate", endDate);

			List<CategoryRevenueDTO> results = query.getResultList();
			log.info("Found {} categories with revenue.", results.size());
			return results;

		} catch (Exception e) {
			log.error("Error fetching category revenue: {}", e.getMessage(), e);
			return new ArrayList<>(); // Return empty list on error
		}
	}

	// ==================== APPROVAL STATUS ====================

	public List<ApprovalResponseDTO> getPendingApprovals(Integer shopId, String entityTypeFilter,
			String actionTypeFilter) {
		List<ApprovalResponseDTO> allApprovals = new ArrayList<>();

		// 1. Fetch ALL pending product approvals
		List<ProductApproval> productApprovals = productApprovalRepository
				.findByProduct_Shop_ShopIdAndStatusOrderByRequestedAtDesc(shopId, "Pending");

		for (ProductApproval pa : productApprovals) {
			ApprovalResponseDTO dto = new ApprovalResponseDTO();
			dto.setApprovalId(pa.getApprovalId());
			dto.setEntityType("PRODUCT"); // Set type explicitly
			dto.setEntityId(pa.getProduct().getProductID());
			dto.setActionType(pa.getActionType());
			dto.setStatus(pa.getStatus());
			dto.setChangeDetails(pa.getChangeDetails());
			dto.setRequestedAt(pa.getRequestedAt());
			dto.setRequestedByName(pa.getRequestedBy().getFullName());
			Optional<Product> productOpt = productRepository.findById(pa.getProduct().getProductID());
			productOpt.ifPresent(product -> dto.setEntityName(product.getProductName()));
			allApprovals.add(dto);
		}

		// 2. Fetch ALL pending promotion approvals
		List<PromotionApproval> promotionApprovals = promotionApprovalRepository
				.findByPromotion_CreatedByShopID_ShopIdAndStatusOrderByRequestedAtDesc(shopId, "Pending");

		for (PromotionApproval pa : promotionApprovals) {
			ApprovalResponseDTO dto = new ApprovalResponseDTO();
			dto.setApprovalId(pa.getApprovalId());
			dto.setEntityType("PROMOTION"); // Set type explicitly
			dto.setEntityId(pa.getPromotion().getPromotionId());
			dto.setActionType(pa.getActionType());
			dto.setStatus(pa.getStatus());
			dto.setChangeDetails(pa.getChangeDetails());
			dto.setRequestedAt(pa.getRequestedAt());
			dto.setRequestedByName(pa.getRequestedBy().getFullName());
			Optional<Promotion> promotionOpt = promotionRepository.findById(pa.getPromotion().getPromotionId());
			promotionOpt.ifPresent(promo -> dto.setEntityName(promo.getPromotionName()));
			allApprovals.add(dto);
		}

		// 3. Fetch ALL pending topping approvals
		List<ToppingApproval> toppingApprovals = toppingApprovalRepository
				.findByShop_ShopIdAndStatusOrderByRequestedAtDesc(shopId, "Pending");

		for (ToppingApproval pa : toppingApprovals) {
			ApprovalResponseDTO dto = new ApprovalResponseDTO();
			dto.setApprovalId(pa.getApprovalId());
			dto.setEntityType("TOPPING"); // Set type
			dto.setEntityId(pa.getTopping() != null ? pa.getTopping().getToppingID() : null);
			dto.setActionType(pa.getActionType());
			dto.setStatus(pa.getStatus());
			dto.setChangeDetails(pa.getChangeDetails());
			dto.setRequestedAt(pa.getRequestedAt());
			dto.setRequestedByName(pa.getRequestedBy().getFullName());
			if (pa.getTopping() != null) {
				dto.setEntityName(pa.getTopping().getToppingName());
			} else if ("CREATE".equals(pa.getActionType())) {
				// Thử đọc tên từ JSON cho trường hợp CREATE
				try {
					ToppingRequestDTO trd = objectMapper.readValue(pa.getChangeDetails(), ToppingRequestDTO.class);
					dto.setEntityName(trd.getToppingName() + " (Mới)");
				} catch (Exception e) {
					dto.setEntityName("(Topping mới)");
				}
			}
			allApprovals.add(dto);
		}

		// 4. Filter the combined list using Streams
		List<ApprovalResponseDTO> filteredApprovals = allApprovals.stream()
				.filter(dto -> !StringUtils.hasText(entityTypeFilter)
						|| dto.getEntityType().equalsIgnoreCase(entityTypeFilter)) // Filter by entity type if provided
				.filter(dto -> !StringUtils.hasText(actionTypeFilter)
						|| dto.getActionType().equalsIgnoreCase(actionTypeFilter)) // Filter by action type if provided
				.sorted(Comparator.comparing(ApprovalResponseDTO::getRequestedAt).reversed()) // Sort AFTER filtering
				.collect(Collectors.toList());

		return filteredApprovals; // Return the filtered and sorted list
	}
}