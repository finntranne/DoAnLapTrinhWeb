package com.alotra.service.vendor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.shop.ShopDashboardDTO;
import com.alotra.entity.shop.Shop;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.product.ProductApprovalRepository;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.product.ToppingApprovalRepository;
import com.alotra.repository.promotion.PromotionApprovalRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.shop.ShopRevenueRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorShopDashboardService {
	private final ShopRepository shopRepository;
	private final ProductRepository productRepository;
	private final ProductApprovalRepository productApprovalRepository;
	private final PromotionApprovalRepository promotionApprovalRepository;
	private final OrderRepository orderRepository;
	private final ShopRevenueRepository shopRevenueRepository;
	private final ToppingApprovalRepository toppingApprovalRepository;

	@PersistenceContext // Inject EntityManager for JPQL
	private EntityManager entityManager;

	// ==================== DASHBOARD ====================

	public ShopDashboardDTO getShopDashboard(Integer shopId) {
		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));

		ShopDashboardDTO dashboard = new ShopDashboardDTO();
		dashboard.setShopId(shopId);
		dashboard.setShopName(shop.getShopName());
		dashboard.setLogoUrl(shop.getLogoURL());

		// Thống kê sản phẩm
		dashboard.setTotalProducts(productRepository.countByShopIdAndStatus(shopId, null).intValue());
		dashboard.setActiveProducts(productRepository.countByShopIdAndStatus(shopId, (byte) 1).intValue());

		// Thống kê phê duyệt đang chờ
		Long pendingProducts = productApprovalRepository.countPendingByShopId(shopId);
		Long pendingPromotions = promotionApprovalRepository.countPendingByShopId(shopId);
		Long pendingToppings = toppingApprovalRepository.countPendingByShopId(shopId);
		dashboard.setPendingApprovals((int) (pendingProducts + pendingPromotions + pendingToppings));

		// Thống kê đơn hàng
		dashboard.setTotalOrders(orderRepository.countByShopId(shopId));
		dashboard.setPendingOrders(orderRepository.countByShopIdAndStatus(shopId, "Pending"));
		dashboard.setDeliveringOrders(orderRepository.countByShopIdAndStatus(shopId, "Delivering"));

		// Thống kê doanh thu
		Double totalRevenue = shopRevenueRepository.getTotalRevenueByShopId(shopId);
		dashboard.setTotalRevenue(BigDecimal.valueOf(totalRevenue != null ? totalRevenue : 0));

		LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
		LocalDateTime endOfMonth = LocalDateTime.now();
		Double monthRevenue = shopRevenueRepository.getRevenueByShopIdAndDateRange(shopId, startOfMonth, endOfMonth);
		dashboard.setThisMonthRevenue(BigDecimal.valueOf(monthRevenue != null ? monthRevenue : 0));

		return dashboard;
	}
}
