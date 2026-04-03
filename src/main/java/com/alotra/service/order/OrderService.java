package com.alotra.service.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alotra.entity.order.Order;
import com.alotra.repository.order.OrderRepository;
import com.alotra.util.OrderPricingUtils;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public BigDecimal getTotalRevenueForCurrentMonth() {
        return getRevenueForMonth(YearMonth.now());
    }

    public double calculateRevenueChangeRate() {
        BigDecimal currentRevenue = getRevenueForMonth(YearMonth.now());
        BigDecimal previousRevenue = getRevenueForMonth(YearMonth.now().minusMonths(1));
        return calculateChangeRate(currentRevenue, previousRevenue);
    }

    public long getTotalOrdersCurrentMonth() {
        return countOrdersForMonth(YearMonth.now());
    }

    public double calculateOrderChangeRate() {
        long currentOrders = countOrdersForMonth(YearMonth.now());
        long previousOrders = countOrdersForMonth(YearMonth.now().minusMonths(1));

        if (previousOrders == 0L) {
            return currentOrders > 0 ? 1.0 : 0.0;
        }
        return (double) (currentOrders - previousOrders) / previousOrders;
    }

    public BigDecimal getTotalProfitCurrentMonth() {
        return getProfitForMonth(YearMonth.now());
    }

    public double calculateProfitChangeRate() {
        BigDecimal currentProfit = getProfitForMonth(YearMonth.now());
        BigDecimal previousProfit = getProfitForMonth(YearMonth.now().minusMonths(1));
        return calculateChangeRate(currentProfit, previousProfit);
    }

    public List<Object[]> getMonthlyShopRanking() {
        YearMonth currentYearMonth = YearMonth.now();
        List<Order> completedOrders = orderRepository.findCompletedOrdersInRange(
                currentYearMonth.atDay(1).atStartOfDay(),
                currentYearMonth.plusMonths(1).atDay(1).atStartOfDay());

        Map<String, BigDecimal> revenueByShop = new LinkedHashMap<>();
        for (Order order : completedOrders) {
            String shopName = order.getShop() != null ? order.getShop().getShopName() : "Unknown";
            BigDecimal orderRevenue = OrderPricingUtils.calculateOrderTotal(order);
            revenueByShop.merge(shopName, orderRevenue, BigDecimal::add);
        }

        return revenueByShop.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> new Object[] { entry.getKey(), entry.getValue() })
                .toList();
    }

    public List<BigDecimal> getRecentMonthlySales(int numberOfMonths) {
        List<BigDecimal> monthlySales = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = numberOfMonths - 1; i >= 0; i--) {
            monthlySales.add(getRevenueForMonth(currentMonth.minusMonths(i)));
        }
        return monthlySales;
    }

    public List<String> getRecentMonthlyLabels(int numberOfMonths) {
        List<String> labels = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = numberOfMonths - 1; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            labels.add("Thang " + targetMonth.getMonthValue());
        }
        return labels;
    }

    private BigDecimal getRevenueForMonth(YearMonth yearMonth) {
        return getOrdersForMonth(yearMonth).stream()
                .map(OrderPricingUtils::calculateOrderTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countOrdersForMonth(YearMonth yearMonth) {
        Long orderCount = orderRepository.countOrdersCreatedInTimeRange(
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.plusMonths(1).atDay(1).atStartOfDay());
        return orderCount != null ? orderCount : 0L;
    }

    private BigDecimal getProfitForMonth(YearMonth yearMonth) {
        return getOrdersForMonth(yearMonth).stream()
                .map(this::calculateCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Order> getOrdersForMonth(YearMonth yearMonth) {
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        return orderRepository.findCompletedOrdersInRange(startDate, endDate);
    }

    private BigDecimal calculateCommission(Order order) {
        BigDecimal total = OrderPricingUtils.calculateOrderTotal(order);
        BigDecimal commissionRate = order.getShop() != null && order.getShop().getCommissionRate() != null
                ? order.getShop().getCommissionRate()
                : BigDecimal.ZERO;
        return total.multiply(commissionRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private double calculateChangeRate(BigDecimal currentValue, BigDecimal previousValue) {
        if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return currentValue.compareTo(BigDecimal.ZERO) > 0 ? 1.0 : 0.0;
        }

        BigDecimal difference = currentValue.subtract(previousValue);
        return difference.divide(previousValue, 4, RoundingMode.HALF_UP).doubleValue();
    }
}
