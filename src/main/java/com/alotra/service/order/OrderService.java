package com.alotra.service.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alotra.repository.order.OrderRepository;

@Service
public class OrderService {
	
	@Autowired
    private OrderRepository orderRepository;

    public BigDecimal getTotalRevenueForCurrentMonth() {
        YearMonth currentYearMonth = YearMonth.now();
        LocalDateTime startDate = currentYearMonth.atDay(1).atStartOfDay(); 

        LocalDateTime endDate = currentYearMonth.plusMonths(1).atDay(1).atStartOfDay(); 

        BigDecimal revenue = orderRepository.calculateMonthlyRevenue(startDate, endDate);

        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    private BigDecimal getRevenueForMonth(YearMonth yearMonth) {
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay(); 
        LocalDateTime endDate = yearMonth.plusMonths(1).atDay(1).atStartOfDay(); 

        BigDecimal revenue = orderRepository.calculateMonthlyRevenue(startDate, endDate);
        
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    public double calculateRevenueChangeRate() {
        BigDecimal currentRevenue = getRevenueForMonth(YearMonth.now());
        BigDecimal previousRevenue = getRevenueForMonth(YearMonth.now().minusMonths(1));

        if (previousRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return currentRevenue.compareTo(BigDecimal.ZERO) > 0 ? 1.0 : 0.0;
        }
        
        BigDecimal difference = currentRevenue.subtract(previousRevenue);
        
        BigDecimal changeRate = difference.divide(previousRevenue, 4, RoundingMode.HALF_UP);
        
        return changeRate.doubleValue();
    }
    
    private Long countOrdersForMonth(YearMonth yearMonth) {
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay(); 
        LocalDateTime endDate = yearMonth.plusMonths(1).atDay(1).atStartOfDay(); 

        Long orderCount = orderRepository.countOrdersCreatedInTimeRange(startDate, endDate);
        
        return orderCount != null ? orderCount : 0L;
    }
    

    public long getTotalOrdersCurrentMonth() {
        return countOrdersForMonth(YearMonth.now());
    }

    public double calculateOrderChangeRate() {
        Long currentOrders = getTotalOrdersCurrentMonth();
        Long previousOrders = countOrdersForMonth(YearMonth.now().minusMonths(1));

        if (previousOrders == 0) {
            return currentOrders > 0 ? 1.0 : 0.0; 
        }

        double difference = currentOrders - previousOrders;
        double changeRate = difference / previousOrders;
        
        return changeRate;
    }
    
    private BigDecimal getProfitForMonth(YearMonth yearMonth) {
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay(); 
        LocalDateTime endDate = yearMonth.plusMonths(1).atDay(1).atStartOfDay(); 

        BigDecimal profit = orderRepository.calculateMonthlyProfit(startDate, endDate);
        
        return profit != null ? profit : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalProfitCurrentMonth() {
        return getProfitForMonth(YearMonth.now());
    }

    public double calculateProfitChangeRate() {
        BigDecimal currentProfit = getTotalProfitCurrentMonth();
        BigDecimal previousProfit = getProfitForMonth(YearMonth.now().minusMonths(1));

        if (previousProfit.compareTo(BigDecimal.ZERO) == 0) {
            return currentProfit.compareTo(BigDecimal.ZERO) > 0 ? 1.0 : 0.0; 
        }
        
        BigDecimal difference = currentProfit.subtract(previousProfit);
        
        BigDecimal changeRate = difference.divide(previousProfit, 4, RoundingMode.HALF_UP);
        
        return changeRate.doubleValue();
    }

    public List<Object[]> getMonthlyShopRanking() {
        YearMonth currentYearMonth = YearMonth.now();
        
        LocalDateTime startDate = currentYearMonth.atDay(1).atStartOfDay(); 
        LocalDateTime endDate = currentYearMonth.plusMonths(1).atDay(1).atStartOfDay(); 

        return orderRepository.getShopRankingByRevenueWithoutDTO(startDate, endDate);
    }
    
    public List<BigDecimal> getRecentMonthlySales(int numberOfMonths) {
        List<BigDecimal> monthlySales = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = numberOfMonths - 1; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            
            LocalDateTime startDate = targetMonth.atDay(1).atStartOfDay(); 
            LocalDateTime endDate = targetMonth.plusMonths(1).atDay(1).atStartOfDay(); 

            BigDecimal revenue = orderRepository.calculateMonthlyRevenue(startDate, endDate);
            monthlySales.add(revenue != null ? revenue : BigDecimal.ZERO);
        }
        
        return monthlySales;
    }

    public List<String> getRecentMonthlyLabels(int numberOfMonths) {
        List<String> labels = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = numberOfMonths - 1; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);

            labels.add("Tháng " + targetMonth.getMonthValue());
        }
        return labels;
    }

}
