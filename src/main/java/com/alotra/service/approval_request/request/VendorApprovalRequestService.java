package com.alotra.service.approval_request.request;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.entity.ApprovalRequest;
import com.alotra.entity.draft.DraftEntity;
import com.alotra.entity.shop.Shop;
import com.alotra.enums.ActionType;
import com.alotra.enums.ApprovalStatus;
import com.alotra.enums.TargetType;
import com.alotra.repository.approval_request.ApprovalRequestRepository;
import com.alotra.repository.approval_request.ProductDraftRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.service.approval_request.request.factory.DraftAbstractFactory;
import com.alotra.service.approval_request.request.factory.DraftFactoryProvider;


@Service
public class VendorApprovalRequestService {

	private final DraftFactoryProvider factoryProvider;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ShopRepository shopRepository;
    
    public VendorApprovalRequestService(DraftFactoryProvider factoryProvider,
            ApprovalRequestRepository approvalRequestRepository,
            ProductDraftRepository productDraftRepository,
            ShopRepository shopRepository) {
		this.factoryProvider = factoryProvider;
		this.approvalRequestRepository = approvalRequestRepository;
		this.shopRepository = shopRepository;
	}
    
    @Transactional
    public void createDraft(Object requestDTO, TargetType targetType, ActionType actionType, Integer userId) {

    	DraftAbstractFactory<? extends DraftEntity> factory = factoryProvider.getFactory(targetType);
    	DraftEntity draft = factory.createDraft(requestDTO, userId);
    	
    	saveDraft(factory, draft);

    	Shop shop = shopRepository.findByUser_Id(userId)
	              .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop"));

        ApprovalRequest request = new ApprovalRequest();
        request.setTargetType(targetType);
        request.setTargetId(draft.getId());
        request.setActionType(actionType);
        request.setStatus(ApprovalStatus.PENDING);
        request.setRequestedBy(shop);
        request.setRequestedAt(LocalDateTime.now());

        approvalRequestRepository.save(request);
    }
    
//    @Transactional
//    public void updateDraft(Object requestDTO, TargetType type, Integer userId) {
//
//        DraftAbstractFactory<? extends DraftEntity> factory = factoryProvider.getFactory(type);
//        DraftEntity draft = factory.createDraft(requestDTO, userId);
//        
//        saveDraft(factory, draft);
//        
//        Shop shop = shopRepository.findByUser_Id(userId)
//	              .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop"));
//
//        ApprovalRequest request = new ApprovalRequest();
//        request.setTargetType(type);
//        request.setTargetId(draft.getId());
//        request.setActionType(ActionType.UPDATE);
//        request.setStatus(ApprovalStatus.PENDING);
//        request.setRequestedBy(shop);
//        request.setRequestedAt(LocalDateTime.now());
//
//        
//        approvalRequestRepository.save(request);
//    }
//    
//    @Transactional
//    public void deleteDraft(Integer targetId, TargetType type, Integer userId) {
//    	
//    	Shop shop = shopRepository.findByUser_Id(userId)
//	              .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop"));
//
//        ApprovalRequest request = new ApprovalRequest();
//        request.setTargetType(type);
//        request.setTargetId(targetId);
//        request.setActionType(ActionType.DELETE);
//        request.setStatus(ApprovalStatus.PENDING);
//        request.setRequestedBy(shop);
//        request.setRequestedAt(LocalDateTime.now());
//
//        approvalRequestRepository.save(request);
//    }
    
    @SuppressWarnings("unchecked")
    private <T extends DraftEntity> void saveDraft(
            DraftAbstractFactory<T> factory,
            DraftEntity draft) {

        factory.save((T) draft);
    }
   
}
