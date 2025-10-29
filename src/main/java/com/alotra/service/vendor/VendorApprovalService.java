package com.alotra.service.vendor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alotra.dto.response.ApprovalResponseDTO;
import com.alotra.dto.topping.ToppingRequestDTO;
import com.alotra.entity.product.Product;
import com.alotra.entity.product.ProductApproval;
import com.alotra.entity.product.ToppingApproval;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.promotion.PromotionApproval;
import com.alotra.repository.product.ProductApprovalRepository;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.product.ToppingApprovalRepository;
import com.alotra.repository.promotion.PromotionApprovalRepository;
import com.alotra.repository.promotion.PromotionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorApprovalService {

	private final ProductRepository productRepository;
	private final ProductApprovalRepository productApprovalRepository;
	private final PromotionRepository promotionRepository;
	private final PromotionApprovalRepository promotionApprovalRepository;
	private final ToppingApprovalRepository toppingApprovalRepository;

	private final ObjectMapper objectMapper;
	@PersistenceContext // Inject EntityManager for JPQL
	private EntityManager entityManager;

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
