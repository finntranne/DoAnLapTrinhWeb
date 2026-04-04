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
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderItem;
import com.alotra.repository.order.OrderRepository;
import com.alotra.util.OrderPricingUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorShopRevenueService {

    private final OrderRepository orderRepository;

    public List<ShopRevenueDTO> getShopRevenue(Integer shopId, LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime effectiveStart = startDate != null ? startDate : LocalDateTime.now().minusDays(14).withHour(0)
                .withMinute(0).withSecond(0).withNano(0);
        LocalDateTime effectiveEnd = endDate != null ? endDate : LocalDateTime.now().plusSeconds(1);

        Map<LocalDateTime, List<Order>> grouped = orderRepository.findCompletedOrdersByShopInRange(shopId,
                        effectiveStart, effectiveEnd)
                .stream()
                .collect(Collectors.groupingBy(order -> order.getOrderDate().toLocalDate().atStartOfDay()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal gross = entry.getValue().stream()
                            .map(OrderPricingUtils::calculateOrderTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal commission = BigDecimal.ZERO;

                    ShopRevenueDTO dto = new ShopRevenueDTO();
                    dto.setDate(entry.getKey());
                    dto.setTotalOrders((long) entry.getValue().size());
                    dto.setOrderAmount(gross);
                    dto.setCommissionAmount(commission);
                    dto.setNetRevenue(gross.subtract(commission));
                    return dto;
                })
                .sorted(Comparator.comparing(ShopRevenueDTO::getDate).reversed())
                .toList();
    }

    public List<CategoryRevenueDTO> getShopRevenueByCategory(Integer shopId, LocalDateTime startDate,
            LocalDateTime endDate) {
        LocalDateTime effectiveStart = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime effectiveEnd = endDate != null ? endDate : LocalDateTime.now().plusSeconds(1);

        Map<String, List<OrderItem>> grouped = orderRepository.findCompletedOrdersByShopInRange(shopId, effectiveStart,
                        effectiveEnd)
                .stream()
                .flatMap(order -> order.getItems().stream())
                .filter(item -> item.getVariant() != null && item.getVariant().getProduct() != null
                        && item.getVariant().getProduct().getCategory() != null)
                .collect(Collectors.groupingBy(item -> item.getVariant().getProduct().getCategory().getCategoryName()));

        List<CategoryRevenueDTO> results = new ArrayList<>();
        for (Map.Entry<String, List<OrderItem>> entry : grouped.entrySet()) {
            BigDecimal gross = entry.getValue().stream()
                    .map(item -> item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal commission = BigDecimal.ZERO;

            CategoryRevenueDTO dto = new CategoryRevenueDTO();
            dto.setCategoryName(entry.getKey());
            dto.setTotalGrossRevenue(gross);
            dto.setTotalNetRevenue(gross.subtract(commission));
            dto.setTotalOrders(entry.getValue().stream()
                    .map(item -> item.getOrder().getOrderID())
                    .distinct()
                    .count());
            results.add(dto);
        }

        results.sort(Comparator.comparing(CategoryRevenueDTO::getTotalGrossRevenue).reversed());
        return results;
    }

}
