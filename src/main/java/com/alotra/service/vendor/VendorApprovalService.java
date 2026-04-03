package com.alotra.service.vendor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alotra.dto.response.ApprovalResponseDTO;
import com.alotra.entity.promotion.PromotionApproval;
import com.alotra.repository.promotion.PromotionApprovalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorApprovalService {

    private final PromotionApprovalRepository promotionApprovalRepository;

    public List<ApprovalResponseDTO> getPendingApprovals(Integer shopId, String entityTypeFilter,
            String actionTypeFilter) {
        if (StringUtils.hasText(entityTypeFilter) && !"PROMOTION".equalsIgnoreCase(entityTypeFilter)) {
            return List.of();
        }

        return promotionApprovalRepository
                .findByPromotion_CreatedByShopID_ShopIdAndStatusOrderByRequestedAtDesc(shopId, "Pending")
                .stream()
                .filter(approval -> !StringUtils.hasText(actionTypeFilter)
                        || approval.getActionType().equalsIgnoreCase(actionTypeFilter))
                .map(this::toDto)
                .toList();
    }

    private ApprovalResponseDTO toDto(PromotionApproval approval) {
        ApprovalResponseDTO dto = new ApprovalResponseDTO();
        dto.setApprovalId(approval.getApprovalId());
        dto.setEntityType("PROMOTION");
        dto.setEntityId(approval.getPromotion() != null ? approval.getPromotion().getPromotionId() : null);
        dto.setEntityName(approval.getPromotion() != null ? approval.getPromotion().getPromotionName() : null);
        dto.setActionType(approval.getActionType());
        dto.setStatus(approval.getStatus());
        dto.setChangeDetails(approval.getChangeDetails());
        dto.setRequestedAt(approval.getRequestedAt());
        dto.setReviewedAt(approval.getReviewedAt());
        dto.setRejectionReason(approval.getRejectionReason());
        dto.setRequestedByName(approval.getRequestedBy() != null ? approval.getRequestedBy().getFullName() : null);
        dto.setReviewedByName(approval.getReviewedBy() != null ? approval.getReviewedBy().getFullName() : null);
        return dto;
    }
}
