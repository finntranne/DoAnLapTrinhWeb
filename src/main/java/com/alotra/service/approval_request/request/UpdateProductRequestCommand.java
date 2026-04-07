package com.alotra.service.approval_request.request;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.dto.product.ProductVariantDTO;
import com.alotra.entity.ApprovalRequest;
import com.alotra.entity.draft.ProductDraft;
import com.alotra.entity.draft.ProductImageDraft;
import com.alotra.entity.draft.ProductVariantDraft;
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.Size;
import com.alotra.entity.product.Topping;
import com.alotra.entity.shop.Shop;
import com.alotra.enums.ActionType;
import com.alotra.enums.ApprovalStatus;
import com.alotra.enums.TargetType;
import com.alotra.repository.approval_request.ApprovalRequestRepository;
import com.alotra.repository.approval_request.ProductDraftRepository;
import com.alotra.repository.product.CategoryRepository;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.product.SizeRepository;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.service.cloudinary.CloudinaryService;

@Service
public class UpdateProductRequestCommand implements ApprovalCommand{
	
	@Autowired
    private ProductDraftRepository productDraftRepository;
	
	@Autowired
    private ApprovalRequestRepository approvalRequestRepository;
	
	@Autowired
    private ShopRepository shopRepository;
	
	@Autowired
    private CategoryRepository categoryRepository;
	
	@Autowired
    private ProductRepository productRepository;
	
	@Autowired
    private ToppingRepository toppingRepository;
	
	@Autowired
    private SizeRepository sizeRepository;
	
	@Autowired
    private CloudinaryService cloudinaryService;
    
	@Override
	public void execute(ProductRequestDTO request, Integer userId) {
		Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop"));
		
		Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy category"));
		
		Product product = getProductDetail(request.getProductId());
		
		ProductDraft draft = new ProductDraft();
		draft.setProduct(product);
        draft.setShop(shop);
        draft.setCategory(category);
        draft.setProductName(request.getProductName());
        draft.setDescription(request.getDescription());
        draft.setStatus((byte) 1);
        
        
        if (request.getAvailableToppingIds() != null && !request.getAvailableToppingIds().isEmpty()) {
            Set<Topping> selectedToppings = new HashSet<>(
                    toppingRepository.findAllById(request.getAvailableToppingIds())
            );
            draft.setAvailableToppings(selectedToppings);
        } else {
            draft.setAvailableToppings(new HashSet<>());
        }
        
        
        Set<ProductImageDraft> imageDrafts = uploadAndBuildImageDrafts(request, draft, userId);
        draft.setImages(imageDrafts);
        
        
        List<ProductVariantDraft> variantDrafts = buildVariantDrafts(request, draft);
        draft.setVariants(variantDrafts);
        
        
        draft.setVariants(variantDrafts);
		
        productDraftRepository.save(draft);

        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setTargetType(TargetType.PRODUCT);
        approvalRequest.setTargetId(draft.getProductDraftID());
        approvalRequest.setActionType(ActionType.UPDATE);
        approvalRequest.setStatus(ApprovalStatus.PENDING);
        approvalRequest.setRequestedBy(shop);
        approvalRequest.setRequestedAt(LocalDateTime.now());

        approvalRequestRepository.save(approvalRequest);
		
	}
	
	private Set<ProductImageDraft> uploadAndBuildImageDrafts(ProductRequestDTO request, ProductDraft draft, Integer userId) {
        Set<ProductImageDraft> imageDrafts = new HashSet<>();

        List<MultipartFile> images = request.getImages();
        if (images == null || images.isEmpty()) {
            return imageDrafts;
        }

        int primaryIndex = request.getPrimaryImageIndex() != null ? request.getPrimaryImageIndex() : 0;
        int displayOrder = 0;

        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String imageUrl;
            try {
                Map<String, String> uploadResult =
                        cloudinaryService.uploadImageAndReturnDetails(file, "products", userId);

                imageUrl = uploadResult.get("secure_url");

                if (imageUrl == null || imageUrl.isBlank()) {
                    throw new RuntimeException("Upload ảnh thành công nhưng không nhận được secure_url");
                }

            } catch (Exception ex) {
                throw new RuntimeException("Không thể upload hình ảnh sản phẩm", ex);
            }

            ProductImageDraft imageDraft = new ProductImageDraft();
            imageDraft.setProductDraft(draft);
            imageDraft.setImageURL(imageUrl);
            imageDraft.setDisplayOrder(displayOrder);
            imageDraft.setIsPrimary(displayOrder == primaryIndex);

            imageDrafts.add(imageDraft);
            
            displayOrder++;
        }

        return imageDrafts;
    }

    private List<ProductVariantDraft> buildVariantDrafts(ProductRequestDTO request, ProductDraft draft) {
        List<ProductVariantDraft> variantDrafts = new ArrayList<>();

        if (request.getVariants() == null) {
            return variantDrafts;
        }

        for (ProductVariantDTO variantDTO : request.getVariants()) {
            Size size = sizeRepository.findById(variantDTO.getSizeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy size với ID: " + variantDTO.getSizeId()));

            ProductVariantDraft variantDraft = new ProductVariantDraft();
            variantDraft.setProductDraft(draft);
            variantDraft.setSize(size);
            variantDraft.setPrice(variantDTO.getPrice());
            
            

            variantDrafts.add(variantDraft);
        }

        return variantDrafts;
    }
    
    @Transactional(readOnly = true)
    public Product getProductDetail(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return product;
    }

}
