package com.alotra.service.vendor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.shop.CategoryRevenueDTO;
import com.alotra.dto.shop.ShopRevenueDTO;
import com.alotra.entity.shop.ShopRevenue;
import com.alotra.repository.shop.ShopRevenueRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorShopRevenueService {

	private final ShopRevenueRepository shopRevenueRepository;

	@PersistenceContext // Inject EntityManager for JPQL
	private EntityManager entityManager;

	// ==================== REVENUE MANAGEMENT ====================

	public List<ShopRevenueDTO> getShopRevenue(Integer shopId, LocalDateTime startDate, LocalDateTime endDate) {
		// Nếu không có filter, mặc định lấy 14 ngày gần nhất
		if (startDate == null && endDate == null) {
			endDate = LocalDateTime.now();
			startDate = endDate.minusDays(14).withHour(0).withMinute(0).withSecond(0);
			log.info("No date filter provided, using default: last 14 days from {} to {}", startDate, endDate);
		} else if (startDate == null) {
			// Nếu chỉ có endDate, lấy 14 ngày trước endDate
			startDate = endDate.minusDays(14).withHour(0).withMinute(0).withSecond(0);
		} else if (endDate == null) {
			// Nếu chỉ có startDate, lấy đến hiện tại
			endDate = LocalDateTime.now();
		}

		log.info("Fetching revenue for shop {} from {} to {}", shopId, startDate, endDate);

		List<ShopRevenue> revenues = shopRevenueRepository.findByShopIdAndDateRange(shopId, startDate, endDate);

		log.info("Found {} revenue records", revenues.size());

		// Group by date
		Map<LocalDateTime, List<ShopRevenue>> groupedByDate = revenues.stream()
				.collect(Collectors.groupingBy(sr -> sr.getRecordedAt().toLocalDate().atStartOfDay()));

		List<ShopRevenueDTO> result = groupedByDate.entrySet().stream().map(entry -> {
			ShopRevenueDTO dto = new ShopRevenueDTO();
			dto.setDate(entry.getKey());
			dto.setTotalOrders((long) entry.getValue().size());
			dto.setOrderAmount(entry.getValue().stream().map(ShopRevenue::getOrderAmount).reduce(BigDecimal.ZERO,
					BigDecimal::add));
			dto.setCommissionAmount(entry.getValue().stream().map(ShopRevenue::getCommissionAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add));
			dto.setNetRevenue(
					entry.getValue().stream().map(ShopRevenue::getNetRevenue).reduce(BigDecimal.ZERO, BigDecimal::add));
			return dto;
		}).sorted(Comparator.comparing(ShopRevenueDTO::getDate).reversed()).collect(Collectors.toList());

		log.info("Grouped into {} days", result.size());

		return result;
	}

	public List<CategoryRevenueDTO> getShopRevenueByCategory(Integer shopId, LocalDateTime startDate,
			LocalDateTime endDate) {
		log.info("Fetching category revenue for shopId: {}, startDate: {}, endDate: {}", shopId, startDate, endDate);

		if (startDate == null) {
			startDate = LocalDateTime.now().minusMonths(1);
		}
		if (endDate == null) {
			endDate = LocalDateTime.now();
		}
		endDate = endDate.withHour(23).withMinute(59).withSecond(59);

		String jpql = """
				    SELECT new com.alotra.dto.shop.CategoryRevenueDTO(
				        c.categoryName,
				        CAST(SUM(od.subtotal) AS java.math.BigDecimal),
				        CAST(SUM(od.subtotal) * (100.0 - COALESCE(s.commissionRate, 5.0)) / 100.0 AS java.math.BigDecimal),
				        COUNT(DISTINCT o.orderID)
				    )
				    FROM Order o
				    JOIN o.orderDetails od
				    JOIN od.variant pv
				    JOIN pv.product p
				    JOIN p.category c
				    JOIN o.shop s
				    WHERE o.shop.shopId = :shopId
				      AND o.orderStatus = 'Completed'
				      AND o.completedAt >= :startDate
				      AND o.completedAt <= :endDate
				    GROUP BY c.categoryName, s.commissionRate
				    ORDER BY SUM(od.subtotal) DESC
				""";

		try {
			TypedQuery<CategoryRevenueDTO> query = entityManager.createQuery(jpql, CategoryRevenueDTO.class);
			query.setParameter("shopId", shopId);
			query.setParameter("startDate", startDate);
			query.setParameter("endDate", endDate);

			List<CategoryRevenueDTO> results = query.getResultList();
			log.info("Found {} categories with revenue.", results.size());
			return results;

		} catch (Exception e) {
			log.error("Error fetching category revenue: {}", e.getMessage(), e);
			return new ArrayList<>(); // Return empty list on error
		}
	}

}
