package com.alotra.service.approval_request.approval.command.product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.alotra.entity.draft.ProductDraft;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;

@Component
public class ProductMapper {
	
	public Product toEntity(ProductDraft draft) {
        Product product = new Product();
        mapDraftToProduct(draft, product);
        return product;
    }

    public void updateEntity(Product product, ProductDraft draft) {
        mapDraftToProduct(draft, product);
    }

	private void mapDraftToProduct(ProductDraft draft, Product product) {
        product.setShop(draft.getShop());
        product.setCategory(draft.getCategory());
        product.setProductName(draft.getProductName());
        product.setDescription(draft.getDescription());
        product.setAvailableToppings(new HashSet<>(draft.getAvailableToppings()));
        product.setStatus((byte) 1); // 1: Đang bán

        if (product.getImages() != null) product.getImages().clear();
        else product.setImages(new HashSet<>());
        
        draft.getImages().forEach(imgDraft -> {
            ProductImage img = new ProductImage();
            img.setProduct(product);
            img.setImageURL(imgDraft.getImageURL());
            img.setDisplayOrder(imgDraft.getDisplayOrder());
            img.setIsPrimary(imgDraft.getIsPrimary());
            product.getImages().add(img);
        });

        Map<Integer, ProductVariant> existingVariants = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getVariantID, v -> v));

        List<ProductVariant> updatedVariants = new ArrayList<>();

        draft.getVariants().forEach(varDraft -> {
            
            ProductVariant v = product.getVariants().stream()
                    .filter(old -> old.getSize().getSizeID().equals(varDraft.getSize().getSizeID()))
                    .findFirst()
                    .orElse(new ProductVariant());
            
            v.setProduct(product);
            v.setSize(varDraft.getSize());
            v.setPrice(varDraft.getPrice());
            
            updatedVariants.add(v);
        });

        product.getVariants().retainAll(updatedVariants); 
        product.getVariants().addAll(updatedVariants.stream()
                .filter(v -> v.getVariantID() == null) 
                .collect(Collectors.toList()));
    }
}
