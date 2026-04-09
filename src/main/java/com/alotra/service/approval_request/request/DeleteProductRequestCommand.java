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
import com.alotra.entity.ApprovalRequest;
import com.alotra.entity.draft.ProductDraft;
import com.alotra.entity.draft.ProductImageDraft;
import com.alotra.entity.draft.ProductVariantDraft;
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;
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
public class DeleteProductRequestCommand implements ApprovalCommand{
	
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
		
		Product product = getProductDetail(request.getProductId());

		Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop"));		
		
		
		ProductDraft draft = new ProductDraft();
		draft.setProduct(product);
        draft.setShop(shop);
        draft.setCategory(product.getCategory());
        draft.setProductName(product.getProductName());
        draft.setDescription(product.getDescription());
        draft.setStatus((byte) 1);
        
        Set<ProductImageDraft> imageDrafts = setImagesFromProduct(product, draft);
        draft.setImages(imageDrafts); 

        List<ProductVariantDraft> variantDrafts = setVariantsFromProduct(product, draft);
        draft.setVariants(variantDrafts);
		
		productDraftRepository.save(draft);
		
		ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setTargetType(TargetType.PRODUCT);
        approvalRequest.setTargetId(draft.getProductDraftID());
        approvalRequest.setActionType(ActionType.DELETE);
        approvalRequest.setStatus(ApprovalStatus.PENDING);
        approvalRequest.setRequestedBy(shop);
        approvalRequest.setRequestedAt(LocalDateTime.now());

        approvalRequestRepository.save(approvalRequest);
		
	}
	
	private Set<ProductImageDraft> setImagesFromProduct(Product product, ProductDraft draft) {
	    Set<ProductImageDraft> imageDrafts = new HashSet<>();

	    if (product.getImages() == null || product.getImages().isEmpty()) {
	        return imageDrafts;
	    }

	    for (ProductImage img : product.getImages()) {
	        ProductImageDraft imageDraft = new ProductImageDraft();

	        imageDraft.setProductDraft(draft);
	        
	        imageDraft.setImageURL(img.getImageURL());
	        imageDraft.setDisplayOrder(img.getDisplayOrder());
	        imageDraft.setIsPrimary(img.getIsPrimary());

	        imageDrafts.add(imageDraft);
	    }

	    return imageDrafts;
	}
	
	private List<ProductVariantDraft> setVariantsFromProduct(Product product, ProductDraft draft) {
	    List<ProductVariantDraft> variantDrafts = new ArrayList<>();

	    if (product.getVariants() == null || product.getVariants().isEmpty()) {
	        return variantDrafts;
	    }

	    for (ProductVariant variant : product.getVariants()) {
	        ProductVariantDraft variantDraft = new ProductVariantDraft();
	        
	        variantDraft.setProductDraft(draft);
	        
	        variantDraft.setSize(variant.getSize());
	        
	        variantDraft.setPrice(variant.getPrice());
	     
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
