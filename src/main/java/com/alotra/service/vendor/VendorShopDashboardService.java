package com.alotra.service.vendor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.shop.ShopDashboardDTO;
import com.alotra.entity.order.Order;
import com.alotra.entity.shop.Shop;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.product.ProductRepository;
import com.alotra.repository.promotion.PromotionApprovalRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.util.OrderPricingUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorShopDashboardService {

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final PromotionApprovalRepository promotionApprovalRepository;
    private final OrderRepository orderRepository;

    public ShopDashboardDTO getShopDashboard(Integer shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        BigDecimal totalRevenue = orderRepository.findCompletedOrdersByShopInRange(shopId,
                        LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.now().plusYears(20))
                .stream()
                .map(OrderPricingUtils::calculateOrderTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                .withNano(0);
        BigDecimal monthRevenue = orderRepository.findCompletedOrdersByShopInRange(shopId, monthStart,
                        LocalDateTime.now().plusSeconds(1))
                .stream()
                .map(OrderPricingUtils::calculateOrderTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ShopDashboardDTO dashboard = new ShopDashboardDTO();
        dashboard.setShopId(shopId);
        dashboard.setShopName(shop.getShopName());
        dashboard.setLogoUrl(shop.getLogoURL());
        dashboard.setTotalProducts(productRepository.countByShopIdAndStatus(shopId, null).intValue());
        dashboard.setActiveProducts(productRepository.countByShopIdAndStatus(shopId, (byte) 1).intValue());
        dashboard.setPendingApprovals(promotionApprovalRepository.countPendingByShopId(shopId).intValue());
        dashboard.setTotalOrders(orderRepository.countByShopId(shopId));
        dashboard.setPendingOrders(orderRepository.countByShopIdAndStatus(shopId, "Pending"));
        dashboard.setDeliveringOrders(orderRepository.countByShopIdAndStatus(shopId, "Delivering"));
        dashboard.setTotalRevenue(totalRevenue);
        dashboard.setThisMonthRevenue(monthRevenue);
        return dashboard;
    }
}
