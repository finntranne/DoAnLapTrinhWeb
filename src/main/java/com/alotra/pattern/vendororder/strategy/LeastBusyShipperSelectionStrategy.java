package com.alotra.pattern.vendororder.strategy;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LeastBusyShipperSelectionStrategy implements ShipperSelectionStrategy {

    private final OrderRepository orderRepository;

    @Override
    public ShipperSelectionType getType() {
        return ShipperSelectionType.LEAST_BUSY;
    }

    @Override
    public User select(ShipperSelectionContext context) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return context.getCandidates().stream()
                .min((left, right) -> compare(left, right, startOfDay, endOfDay))
                .orElseThrow(() -> new IllegalStateException("No shipper available for least-busy selection"));
    }

    private int compare(User left, User right, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        long leftActiveOrders = orderRepository.countActiveAssignments(left.getId());
        long rightActiveOrders = orderRepository.countActiveAssignments(right.getId());
        int activeOrderComparison = Long.compare(leftActiveOrders, rightActiveOrders);
        if (activeOrderComparison != 0) {
            return activeOrderComparison;
        }

        long leftOrdersToday = orderRepository.countByShipper_IdAndOrderDateBetween(left.getId(), startOfDay, endOfDay);
        long rightOrdersToday = orderRepository.countByShipper_IdAndOrderDateBetween(right.getId(), startOfDay, endOfDay);
        int todayOrderComparison = Long.compare(leftOrdersToday, rightOrdersToday);
        if (todayOrderComparison != 0) {
            return todayOrderComparison;
        }

        LocalDateTime leftLastLogin = left.getLastLoginAt() != null ? left.getLastLoginAt() : LocalDateTime.MIN;
        LocalDateTime rightLastLogin = right.getLastLoginAt() != null ? right.getLastLoginAt() : LocalDateTime.MIN;
        return rightLastLogin.compareTo(leftLastLogin);
    }
}
