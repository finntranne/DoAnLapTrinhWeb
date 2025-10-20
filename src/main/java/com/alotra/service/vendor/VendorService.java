package com.alotra.service.vendor;

import com.alotra.dto.*;
import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.dto.product.ProductStatisticsDTO;
import com.alotra.dto.product.ProductVariantDTO;
import com.alotra.dto.promotion.PromotionRequestDTO;
import com.alotra.dto.response.ApprovalResponseDTO;
import com.alotra.dto.shop.ShopDashboardDTO;
import com.alotra.dto.shop.ShopOrderDTO;
import com.alotra.dto.shop.ShopRevenueDTO;
import com.alotra.entity.*;
import com.alotra.entity.order.Order;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductApproval;
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;
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
        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new RuntimeException("Shop not found"));
        
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
    
    public Page<ProductStatisticsDTO> getShopProducts(Integer shopId, Byte status, String search, Pageable pageable) {
        Page<Product> products = productRepository.searchShopProducts(shopId, status, search, pageable);
        
        return products.map(product -> {
            ProductStatisticsDTO dto = new ProductStatisticsDTO();
            dto.setProductId(product.getProductID());
            dto.setProductName(product.getProductName());
            dto.setSoldCount(product.getSoldCount());
            dto.setAverageRating(product.getAverageRating());
            dto.setTotalReviews(product.getTotalReviews());
            dto.setViewCount(product.getViewCount());
            dto.setStatus(product.getStatus() == 1 ? "Active" : "Inactive");
            
            // Lấy primary image
            product.getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .findFirst()
                .ifPresent(img -> dto.setPrimaryImageUrl(img.getImageURL()));
            
            // Lấy giá thấp nhất
            product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .ifPresent(dto::setMinPrice);
            
            // Kiểm tra trạng thái phê duyệt
            productApprovalRepository.findByProduct_ProductIdAndStatusAndActionType(
                product.getProductID(), "Pending", null)
                .ifPresent(approval -> dto.setApprovalStatus("Pending: " + approval.getActionType()));
            
            return dto;
        });
    }
    
    public void requestProductCreation(Integer shopId, ProductRequestDTO request, Integer userId) 
            throws JsonProcessingException {
        
        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new RuntimeException("Shop not found"));
        
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
        
        // Upload và lưu hình ảnh
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                MultipartFile file = request.getImages().get(i);
                if (!file.isEmpty()) {
                    String imageUrl = cloudinaryService.uploadImage(file);
                    
                    ProductImage productImage = new ProductImage();
                    productImage.setProduct(product);
                    productImage.setImageURL(imageUrl);
                    productImage.setIsPrimary(i == request.getPrimaryImageIndex());
                    productImage.setDisplayOrder(i);
                    productImageRepository.save(productImage);
                }
            }
        }
        
        // Tạo variants
        for (ProductVariantDTO variantDTO : request.getVariants()) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(sizeRepository.findById(variantDTO.getSizeId())
                .orElseThrow(() -> new RuntimeException("Size not found")));
            variant.setPrice(variantDTO.getPrice());
            variant.setStock(variantDTO.getStock());
            variant.setSku(variantDTO.getSku());
            productVariantRepository.save(variant);
        }
        
        // Tạo yêu cầu phê duyệt
        ProductApproval approval = new ProductApproval();
        approval.setProduct(product);
        approval.setActionType("CREATE");
        approval.setStatus("Pending");
        approval.setChangeDetails(objectMapper.writeValueAsString(request));
        approval.setRequestedBy(userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found")));
        approval.setRequestedAt(LocalDateTime.now());
        
        productApprovalRepository.save(approval);
        
        // Gửi thông báo cho admin
        notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
        
        log.info("Product creation requested - Product ID: {}, Shop ID: {}", product.getProductID(), shopId);
    }
    
    public void requestProductUpdate(Integer shopId, ProductRequestDTO request, Integer userId) 
            throws JsonProcessingException {
        
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (!product.getShop().getShopId().equals(shopId)) {
            throw new RuntimeException("Unauthorized: Product does not belong to this shop");
        }
        
        // Kiểm tra xem có yêu cầu pending nào không
        Optional<ProductApproval> existingApproval = productApprovalRepository
            .findByProduct_ProductIdAndStatusAndActionType(product.getProductID(), "Pending", "UPDATE");
        
        if (existingApproval.isPresent()) {
            throw new RuntimeException("There is already a pending update request for this product");
        }
        
        // Tạo yêu cầu phê duyệt
        ProductApproval approval = new ProductApproval();
        approval.setProduct(product);
        approval.setActionType("UPDATE");
        approval.setStatus("Pending");
        approval.setChangeDetails(objectMapper.writeValueAsString(request));
        approval.setRequestedBy(userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found")));
        approval.setRequestedAt(LocalDateTime.now());
        
        productApprovalRepository.save(approval);
        
        notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
        
        log.info("Product update requested - Product ID: {}, Shop ID: {}", product.getProductID(), shopId);
    }
    
    public void requestProductDeletion(Integer shopId, Integer productId, Integer userId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (!product.getShop().getShopId().equals(shopId)) {
            throw new RuntimeException("Unauthorized: Product does not belong to this shop");
        }
        
        // Tạo yêu cầu phê duyệt
        ProductApproval approval = new ProductApproval();
        approval.setProduct(product);
        approval.setActionType("DELETE");
        approval.setStatus("Pending");
        approval.setRequestedBy(userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found")));
        approval.setRequestedAt(LocalDateTime.now());
        
        productApprovalRepository.save(approval);
        
        notificationService.notifyAdminsAboutNewApproval("PRODUCT", product.getProductID());
        
        log.info("Product deletion requested - Product ID: {}, Shop ID: {}", product.getProductID(), shopId);
    }
    
    // ==================== PROMOTION MANAGEMENT ====================
    
    public Page<Promotion> getShopPromotions(Integer shopId, Byte status, Pageable pageable) {
        return promotionRepository.findShopPromotionsByStatus(shopId, status, pageable);
    }
    
    public void requestPromotionCreation(Integer shopId, PromotionRequestDTO request, Integer userId) 
            throws JsonProcessingException {
        
        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new RuntimeException("Shop not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
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
        
        // Kiểm tra pending request
        Optional<PromotionApproval> existingApproval = promotionApprovalRepository
            .findByPromotion_PromotionIdAndStatusAndActionType(promotion.getPromotionId(), "Pending", "UPDATE");
        
        if (existingApproval.isPresent()) {
            throw new RuntimeException("There is already a pending update request for this promotion");
        }
        
        // Tạo yêu cầu phê duyệt
        PromotionApproval approval = new PromotionApproval();
        approval.setPromotion(promotion);
        approval.setActionType("UPDATE");
        approval.setStatus("Pending");
        approval.setChangeDetails(objectMapper.writeValueAsString(request));
        approval.setRequestedBy(userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found")));
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
        
        // Tạo yêu cầu phê duyệt
        PromotionApproval approval = new PromotionApproval();
        approval.setPromotion(promotion);
        approval.setActionType("DELETE");
        approval.setStatus("Pending");
        approval.setRequestedBy(userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found")));
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
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.getShop().getShopId().equals(shopId)) {
            throw new RuntimeException("Unauthorized: Order does not belong to this shop");
        }
        
        return order;
    }
    
    public void updateOrderStatus(Integer shopId, Integer orderId, String newStatus, Integer userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
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
        
        return groupedByDate.entrySet().stream()
            .map(entry -> {
                ShopRevenueDTO dto = new ShopRevenueDTO();
                dto.setDate(entry.getKey());
                dto.setTotalOrders((long) entry.getValue().size());
                dto.setOrderAmount(entry.getValue().stream()
                    .map(ShopRevenue::getOrderAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
                dto.setCommissionAmount(entry.getValue().stream()
                    .map(ShopRevenue::getCommissionAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
                dto.setNetRevenue(entry.getValue().stream()
                    .map(ShopRevenue::getNetRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
                return dto;
            })
            .sorted(Comparator.comparing(ShopRevenueDTO::getDate).reversed())
            .collect(Collectors.toList());
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