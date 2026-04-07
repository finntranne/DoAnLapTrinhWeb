package com.alotra.service.approval_request.approval;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alotra.entity.ApprovalRequest;
import com.alotra.entity.draft.ProductDraft;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductImage;
import com.alotra.entity.product.ProductVariant;
import com.alotra.enums.ActionType;
import com.alotra.enums.ApprovalStatus;
import com.alotra.repository.approval_request.ProductDraftRepository;
import com.alotra.repository.product.ProductRepository;

@Component("PENDING_STATE")
public class PendingApprovalState implements ApprovalState{
	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private ProductDraftRepository productDraftRepository;

	@Override
	public void approve(ApprovalRequest request) {
        ProductDraft draft = productDraftRepository.findById(request.getTargetId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản thảo sản phẩm"));

        switch (request.getActionType()) {
            case CREATE:
                handleCreate(draft);
                break;
            case UPDATE:
                handleUpdate(draft);
                break;
            case DELETE:
                handleDelete(draft);
                break;
        }

        request.setStatus(ApprovalStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        
        draft.setStatus((byte) 2); 
        productDraftRepository.save(draft);
    }
	@Override
	public void reject(ApprovalRequest request, String reason) {
		request.setStatus(ApprovalStatus.REJECTED);
        request.setReason(reason);
        request.setReviewedAt(LocalDateTime.now());
        
        ProductDraft draft = productDraftRepository.findById(request.getTargetId()).orElse(null);
        if (draft != null) {
            draft.setStatus((byte) 3);
            productDraftRepository.save(draft);
        }
	}

	@Override
	public String getStateName() {
		return "PENDING";
	}
	
	
	private void handleCreate(ProductDraft draft) {
        Product product = new Product();
        mapDraftToProduct(draft, product);
        productRepository.save(product);
    }

    private void handleUpdate(ProductDraft draft) {
        Product existingProduct = draft.getProduct();
        if (existingProduct == null) throw new RuntimeException("Không tìm thấy sản phẩm gốc để cập nhật");
        
        mapDraftToProduct(draft, existingProduct);
        productRepository.save(existingProduct);
    }

    private void handleDelete(ProductDraft draft) {
        Product existingProduct = draft.getProduct();
        if (existingProduct == null) throw new RuntimeException("Không tìm thấy sản phẩm gốc để xóa");
        
        existingProduct.setStatus((byte) 0);
        productRepository.save(existingProduct);
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

        if (product.getVariants() != null) product.getVariants().clear();
        else product.setVariants(new ArrayList<>());

        draft.getVariants().forEach(varDraft -> {
            ProductVariant v = new ProductVariant();
            v.setProduct(product);
            v.setSize(varDraft.getSize());
            v.setPrice(varDraft.getPrice());
            product.getVariants().add(v);
        });
    }

}
