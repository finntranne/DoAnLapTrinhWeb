package com.alotra.service.approval_request.request.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.dto.product.ProductVariantDTO;
import com.alotra.entity.draft.ProductDraft;
import com.alotra.entity.draft.ProductImageDraft;
import com.alotra.entity.draft.ProductVariantDraft;
import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.Size;
import com.alotra.entity.product.Topping;
import com.alotra.entity.shop.Shop;
import com.alotra.repository.product.CategoryRepository;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.product.SizeRepository;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.service.cloudinary.CloudinaryService;

@Component
public class ProductDraftBuilder implements DraftBuilder<ProductDraft, ProductRequestDTO>{
	
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
	public ProductDraft build(ProductRequestDTO request, Integer userId) {
		Shop shop = shopRepository.findById(request.getShopId())
	              .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop"));
			
		Category category = categoryRepository.findById(request.getCategoryId())
              .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy category"));
		
		Product product = null;
	    if (request.getProductId() != null) {
	        product = productRepository.findById(request.getProductId()).orElse(null);
	    }
		
		ProductDraft draft = new ProductDraft();
		  draft.setShop(shop);
		  draft.setCategory(category);
		  draft.setProductName(request.getProductName());
		  draft.setDescription(request.getDescription());
		  draft.setStatus((byte) 1);
		  draft.setProduct(product);
		  
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
      
      return draft;
	}

	private Set<ProductImageDraft> uploadAndBuildImageDrafts(ProductRequestDTO request, ProductDraft draft, Integer userId) {
	    Set<ProductImageDraft> imageDrafts = new HashSet<>();
	    int displayOrder = 0;

	    List<MultipartFile> newImages = request.getImages();
	    
	    boolean hasActualNewUploads = false;
	    if (newImages != null) {
	        for (MultipartFile file : newImages) {
	            if (file != null && !file.isEmpty()) { // .isEmpty() kiểm tra file có nội dung không
	                hasActualNewUploads = true;
	                break;
	            }
	        }
	    } 
	    if (hasActualNewUploads) {
	        for (MultipartFile file : newImages) {
	            if (file == null || file.isEmpty()) continue;
	            try {
	                Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(file, "products", userId);
	                String imageUrl = uploadResult.get("secure_url");

	                ProductImageDraft imageDraft = new ProductImageDraft();
	                imageDraft.setProductDraft(draft);
	                imageDraft.setImageURL(imageUrl);
	                imageDraft.setDisplayOrder(displayOrder);
	                imageDraft.setIsPrimary(displayOrder == request.getPrimaryImageIndex());

	                imageDrafts.add(imageDraft);
	                displayOrder++;
	            } catch (Exception ex) {
	                throw new RuntimeException("Không thể upload hình ảnh sản phẩm", ex);
	            }
	        }
	    } else {  
	    	if (request.getExistingImageUrls() != null) {
		        for (String oldUrl : request.getExistingImageUrls()) {
		            if (oldUrl != null && !oldUrl.isBlank()) {
		                ProductImageDraft imageDraft = new ProductImageDraft();
		                imageDraft.setProductDraft(draft);
		                imageDraft.setImageURL(oldUrl);
		                imageDraft.setDisplayOrder(displayOrder);
		                imageDraft.setIsPrimary(displayOrder == request.getPrimaryImageIndex());
		                
		                imageDrafts.add(imageDraft);
		                displayOrder++;
		            }
		        }
		    }
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

}
