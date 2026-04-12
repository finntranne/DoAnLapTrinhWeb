package com.alotra.pattern.vendororder.strategy;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecentlyActiveShipperSelectionStrategy implements ShipperSelectionStrategy {

    private final OrderRepository orderRepository;

    @Override
    public ShipperSelectionType getType() {
        return ShipperSelectionType.RECENTLY_ACTIVE;
    }

    @Override
    public User select(ShipperSelectionContext context) {
        return context.getCandidates().stream()
                .max((left, right) -> compare(left, right))
                .orElseThrow(() -> new IllegalStateException("No shipper available for recent-activity selection"));
    }

    private int compare(User left, User right) {
        LocalDateTime leftLastLogin = left.getLastLoginAt() != null ? left.getLastLoginAt() : LocalDateTime.MIN;
        LocalDateTime rightLastLogin = right.getLastLoginAt() != null ? right.getLastLoginAt() : LocalDateTime.MIN;
        int loginComparison = leftLastLogin.compareTo(rightLastLogin);
        if (loginComparison != 0) {
            return loginComparison;
        }

        long leftActiveOrders = orderRepository.countActiveAssignments(left.getId());
        long rightActiveOrders = orderRepository.countActiveAssignments(right.getId());
        return Long.compare(rightActiveOrders, leftActiveOrders);
    }
}
