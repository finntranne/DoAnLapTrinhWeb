package com.alotra.service.vendor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Page;
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
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Review;
import com.alotra.entity.product.Size;
import com.alotra.entity.product.Topping;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.promotion.PromotionProduct;
import com.alotra.entity.promotion.PromotionProductId;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.repository.product.CategoryRepository;
import com.alotra.repository.product.ProductImageRepository;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.product.ProductVariantRepository;
import com.alotra.repository.product.ReviewRepository;
import com.alotra.repository.product.SizeRepository;
import com.alotra.repository.promotion.PromotionProductRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.cloudinary.CloudinaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorProductService {

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final SizeRepository sizeRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ProductStatisticsDTO> getShopProducts(Integer shopId, Byte status, Integer categoryId,
            String approvalStatus, String search, org.springframework.data.domain.Pageable pageable) {
        return productRepository.searchShopProducts(shopId, status, categoryId, approvalStatus, search, pageable)
                .map(this::toStatisticsDto);
    }

    @Transactional(readOnly = true)
    public Product getProductDetail(Integer shopId, Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!product.getShop().getShopId().equals(shopId)) {
            throw new RuntimeException("Unauthorized: Product does not belong to this shop");
        }
        return product;
    }

    public void requestProductCreation(Integer shopId, ProductRequestDTO request, Integer userId,
            Set<Topping> selectedToppings) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        Product product = new Product();
        product.setShop(shop);
        product.setCategory(loadCategory(request.getCategoryId()));
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setStatus((byte) 1);
        product.setAvailableToppings(selectedToppings != null ? selectedToppings : new HashSet<>());

        product = productRepository.save(product);
        syncVariants(product, request.getVariants());
        syncImages(product, request.getImages(), request.getPrimaryImageIndex(), userId, false);
        replaceProductDiscount(product, request.getDiscountPercentage(), shopId, userId);
    }

    public void requestProductUpdate(Integer shopId, ProductRequestDTO request, Integer userId,
            Set<Topping> selectedToppings) {
        Product product = getProductDetail(shopId, request.getProductId());

        product.setCategory(loadCategory(request.getCategoryId()));
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setAvailableToppings(selectedToppings != null ? selectedToppings : new HashSet<>());
        productRepository.save(product);

        syncVariants(product, request.getVariants());
        syncImages(product, request.getImages(), request.getPrimaryImageIndex(), userId, true);
        replaceProductDiscount(product, request.getDiscountPercentage(), shopId, userId);
    }

    public void requestProductDeletion(Integer shopId, Integer productId, Integer userId) {
        Product product = getProductDetail(shopId, productId);
        product.setStatus((byte) 0);
        productRepository.save(product);
        replaceProductDiscount(product, null, shopId, userId);
    }

    @Transactional(readOnly = true)
    public List<SimpleCategoryDTO> getAllCategoriesSimple() {
        return categoryRepository.findAll().stream()
                .map(category -> new SimpleCategoryDTO(category.getCategoryID(), category.getCategoryName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SimpleSizeDTO> getAllSizesSimple() {
        return sizeRepository.findAllByOrderBySizeNameAsc().stream()
                .map(size -> new SimpleSizeDTO(size.getSizeID(), size.getSizeName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ProductRequestDTO convertProductToDTO(Product product) {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setProductId(product.getProductID());
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getCategoryID() : null);
        dto.setProductName(product.getProductName());
        dto.setDescription(product.getDescription());
        dto.setVariants(product.getVariants().stream()
                .sorted(Comparator.comparing(ProductVariant::getVariantID))
                .map(this::toVariantDto)
                .toList());
        dto.setExistingImageUrls(product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getDisplayOrder))
                .map(ProductImage::getImageURL)
                .toList());
        dto.setAvailableToppingIds(product.getAvailableToppings().stream()
                .map(Topping::getToppingID)
                .collect(java.util.stream.Collectors.toSet()));

        List<ProductImage> images = product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getDisplayOrder))
                .toList();
        for (int i = 0; i < images.size(); i++) {
            if (Boolean.TRUE.equals(images.get(i).getIsPrimary())) {
                dto.setPrimaryImageIndex(i);
                break;
            }
        }

        dto.setDiscountPercentage(getCurrentDiscount(product));
        return dto;
    }

    private ProductStatisticsDTO toStatisticsDto(Product product) {
        ProductStatisticsDTO dto = new ProductStatisticsDTO();
        dto.setProductId(product.getProductID());
        dto.setProductName(product.getProductName());
        dto.setPrimaryImageUrl(product.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .map(ProductImage::getImageURL)
                .findFirst()
                .orElseGet(() -> product.getImages().stream()
                        .sorted(Comparator.comparing(ProductImage::getDisplayOrder))
                        .map(ProductImage::getImageURL)
                        .findFirst()
                        .orElse(null)));
        dto.setSoldCount(0);
        Double avgRating = reviewRepository.calculateAverageRating(product.getProductID());
        dto.setAverageRating(avgRating != null ? BigDecimal.valueOf(avgRating) : BigDecimal.ZERO);
        dto.setTotalReviews(reviewRepository.countByProductId(product.getProductID()).intValue());
        dto.setViewCount(0);
        dto.setMinPrice(product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO));
        dto.setStatus(product.getStatus() != null && product.getStatus() == 1 ? "Dang hoat dong" : "Khong hoat dong");
        dto.setApprovalStatus("Approved");
        dto.setDiscountPercentage(getCurrentDiscount(product));
        return dto;
    }

    private ProductVariantDTO toVariantDto(ProductVariant variant) {
        ProductVariantDTO dto = new ProductVariantDTO();
        dto.setVariantId(variant.getVariantID());
        dto.setSizeId(variant.getSize() != null ? variant.getSize().getSizeID() : null);
        dto.setPrice(variant.getPrice());
        dto.setStock(0);
        dto.setSku(null);
        return dto;
    }

    private Category loadCategory(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    private Size loadSize(Integer sizeId) {
        return sizeRepository.findById(sizeId)
                .orElseThrow(() -> new RuntimeException("Size not found"));
    }

    private void syncVariants(Product product, List<ProductVariantDTO> variants) {
        List<ProductVariant> managedVariants = product.getVariants();
        managedVariants.clear();

        for (ProductVariantDTO variantDTO : variants) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(loadSize(variantDTO.getSizeId()));
            variant.setPrice(variantDTO.getPrice());
            managedVariants.add(variant);
        }
    }

    private void syncImages(Product product, List<MultipartFile> images, Integer primaryImageIndex, Integer userId,
            boolean replaceOnlyWhenNewImages) {
        boolean hasNewImages = images != null && images.stream().anyMatch(file -> file != null && !file.isEmpty());
        if (replaceOnlyWhenNewImages && !hasNewImages) {
            return;
        }

        productImageRepository.deleteByProduct_ProductID(product.getProductID());

        if (!hasNewImages) {
            return;
        }

        int primaryIndex = primaryImageIndex != null ? primaryImageIndex : 0;
        int displayOrder = 0;
        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String imageUrl;
            try {
                imageUrl = cloudinaryService.uploadImageAndReturnDetails(file, "products", userId).get("secure_url");
            } catch (Exception ex) {
                throw new RuntimeException("Khong the upload hinh anh san pham", ex);
            }
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageURL(imageUrl);
            image.setDisplayOrder(displayOrder);
            image.setIsPrimary(displayOrder == primaryIndex);
            productImageRepository.save(image);
            displayOrder++;
        }
    }

    private Integer getCurrentDiscount(Product product) {
        return promotionProductRepository.findByProduct_ProductID(product.getProductID()).stream()
                .filter(link -> link.getPromotion() != null)
                .filter(link -> "PRODUCT".equalsIgnoreCase(link.getPromotion().getPromotionType()))
                .filter(link -> link.getPromotion().getStatus() != null && link.getPromotion().getStatus() == 1)
                .filter(link -> link.getPromotion().getEndDate() != null
                        && link.getPromotion().getEndDate().isAfter(LocalDateTime.now()))
                .map(PromotionProduct::getDiscountPercentage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void replaceProductDiscount(Product product, Integer discountPercentage, Integer shopId, Integer userId) {
        List<PromotionProduct> existingLinks = promotionProductRepository.findByProduct_ProductID(product.getProductID())
                .stream()
                .filter(link -> link.getPromotion() != null)
                .filter(link -> "PRODUCT".equalsIgnoreCase(link.getPromotion().getPromotionType()))
                .toList();

        for (PromotionProduct existingLink : existingLinks) {
            promotionProductRepository.delete(existingLink);
            Promotion promotion = existingLink.getPromotion();
            if (promotion != null) {
                promotionRepository.delete(promotion);
            }
        }

        if (discountPercentage == null || discountPercentage <= 0) {
            return;
        }

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Promotion promotion = new Promotion();
//        promotion.setCreatedByUserID(user);
        promotion.setCreatedByShopID(shop);
        promotion.setPromotionName("Discount for " + product.getProductName());
        promotion.setDescription("Auto-generated product discount");
        promotion.setPromoCode("PRODUCT_" + product.getProductID() + "_" + System.currentTimeMillis());
        promotion.setPromotionType("PRODUCT");
        promotion.setDiscountType("Percentage");
        promotion.setDiscountValue(BigDecimal.valueOf(discountPercentage));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusYears(10));
        promotion.setUsageLimit(null);
        promotion.setStatus((byte) 1);
        promotion = promotionRepository.save(promotion);

        PromotionProduct link = new PromotionProduct();
        link.setId(new PromotionProductId(promotion.getPromotionId(), product.getProductID()));
        link.setPromotion(promotion);
        link.setProduct(product);
        link.setDiscountPercentage(discountPercentage);
        promotionProductRepository.save(link);
    }
}
