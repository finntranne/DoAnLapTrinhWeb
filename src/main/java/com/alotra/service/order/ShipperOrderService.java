package com.alotra.service.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.order.ShipperOrderDTO;
import com.alotra.dto.shipper.ShipperDashboardDTO;
import com.alotra.dto.shipper.ShipperInfoDTO;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderHistory;
import com.alotra.entity.order.OrderShippingHistory;
import com.alotra.entity.order.Payment;
import com.alotra.enums.PaymentMethod;
import com.alotra.enums.PaymentStatus;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderHistoryRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.OrderShippingHistoryRepository;
import com.alotra.repository.order.PaymentRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.notification.NotificationService;
import com.alotra.util.OrderPricingUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipperOrderService {

    private final OrderRepository orderRepository;
    private final OrderShippingHistoryRepository shippingHistoryRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public ShipperInfoDTO getShipperInfo(Integer shipperId) {
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay shipper"));

        ShipperInfoDTO dto = new ShipperInfoDTO();
        dto.setUserId(shipper.getId());
        dto.setFullName(shipper.getFullName());
        dto.setEmail(shipper.getEmail());
        dto.setPhoneNumber(shipper.getPhoneNumber());
        dto.setAvatarURL(shipper.getAvatarURL());

        orderRepository.findShipperOrders(shipperId, null).stream()
                .filter(order -> order.getShop() != null)
                .findFirst()
                .ifPresent(order -> {
                    dto.setShopName(order.getShop().getShopName());
                    dto.setShopPhone(order.getShop().getPhoneNumber());
                    dto.setShopAddress(OrderPricingUtils.formatAddress(order.getShop().getAddress()));
                });

        if (dto.getShopName() == null) {
            dto.setShopName("Chua co don duoc giao");
            dto.setShopPhone("");
            dto.setShopAddress("");
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public ShipperDashboardDTO getDashboardStats(Integer shipperId) {
        List<Order> orders = orderRepository.findShipperOrders(shipperId, null);

        long assignedCount = orders.stream()
                .map(order -> getCurrentShippingStatus(order, shipperId))
                .filter("Assigned"::equals)
                .count();

        long deliveringCount = orders.stream()
                .map(order -> getCurrentShippingStatus(order, shipperId))
                .filter(this::isInDeliveryFlow)
                .count();

        LocalDate today = LocalDate.now();
        long completedTodayCount = orders.stream()
                .filter(order -> "Delivered".equals(getCurrentShippingStatus(order, shipperId)))
                .map(order -> getLatestShippingHistory(order.getOrderID(), shipperId).orElse(null))
                .filter(history -> history != null && history.getTimestamp() != null)
                .filter(history -> history.getTimestamp().toLocalDate().equals(today))
                .count();

        LocalDateTime startOfWeek = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        long weeklyCount = orderRepository.countByShipper_IdAndOrderDateBetween(shipperId, startOfWeek, LocalDateTime.now());

        long totalCompletedCount = orders.stream()
                .filter(order -> "Delivered".equals(getCurrentShippingStatus(order, shipperId)))
                .count();

        long totalAssignedEver = orderRepository.countByShipper_Id(shipperId);
        double successRate = totalAssignedEver == 0 ? 0.0 : Math.round((totalCompletedCount * 10000.0) / totalAssignedEver) / 100.0;

        long workingDays = orders.stream()
                .map(order -> getLatestShippingHistory(order.getOrderID(), shipperId).orElse(null))
                .filter(history -> history != null && history.getTimestamp() != null)
                .map(OrderShippingHistory::getTimestamp)
                .min(Comparator.naturalOrder())
                .map(firstAssigned -> ChronoUnit.DAYS.between(firstAssigned.toLocalDate(), today) + 1)
                .orElse(0L);

        return new ShipperDashboardDTO(
                assignedCount,
                deliveringCount,
                completedTodayCount,
                weeklyCount,
                totalCompletedCount,
                successRate,
                workingDays);
    }

    @Transactional(readOnly = true)
    public List<ShipperOrderDTO> getPendingOrders(Integer shipperId, int limit) {
        return orderRepository.findShipperOrders(shipperId, null).stream()
                .map(order -> toDto(order, shipperId))
                .filter(dto -> "Assigned".equals(dto.getCurrentShippingStatus()))
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShipperOrderDTO> getDeliveringOrders(Integer shipperId, int limit) {
        return orderRepository.findShipperOrders(shipperId, null).stream()
                .map(order -> toDto(order, shipperId))
                .filter(dto -> isInDeliveryFlow(dto.getCurrentShippingStatus()))
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ShipperOrderDTO> getShipperOrders(Integer shipperId, String status, String searchQuery, Pageable pageable) {
        String normalizedStatus = normalizeFilterStatus(status);

        List<ShipperOrderDTO> filtered = orderRepository.findShipperOrders(shipperId, null).stream()
                .map(order -> toDto(order, shipperId))
                .filter(dto -> matchesStatus(dto, normalizedStatus))
                .filter(dto -> matchesSearch(dto, searchQuery))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<ShipperOrderDTO> pageContent = start >= filtered.size() ? List.of() : filtered.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public long countOrdersByStatus(Integer shipperId, String status) {
        String normalizedStatus = normalizeFilterStatus(status);
        return orderRepository.findShipperOrders(shipperId, null).stream()
                .map(order -> toDto(order, shipperId))
                .filter(dto -> matchesStatus(dto, normalizedStatus))
                .count();
    }

    @Transactional(readOnly = true)
    public Optional<ShipperOrderDTO> getOrderDetail(Integer orderId, Integer shipperId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getShipper() != null && shipperId.equals(order.getShipper().getId()))
                .map(order -> toDto(order, shipperId));
    }

    @Transactional(readOnly = true)
    public List<OrderShippingHistory> getShippingHistory(Integer orderId, Integer shipperId) {
        return shippingHistoryRepository.findByOrder_OrderIDAndShipper_IdOrderByTimestampDesc(orderId, shipperId);
    }

    public void createInitialShippingHistory(Integer orderId, Integer shipperId, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang"));
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay shipper"));

        OrderShippingHistory history = new OrderShippingHistory();
        history.setOrder(order);
        history.setShipper(shipper);
        history.setStatus("Assigned");
        history.setNotes(notes != null ? notes : "Don hang duoc gan cho shipper");
        history.setTimestamp(LocalDateTime.now());
        shippingHistoryRepository.save(history);
    }

    public void updateShippingStatus(Integer orderId, Integer shipperId, String status, String notes, String imageURL) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang"));

        if (order.getShipper() == null || !shipperId.equals(order.getShipper().getId())) {
            throw new RuntimeException("Ban khong duoc phan cong don hang nay");
        }

        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay shipper"));

        OrderShippingHistory history = new OrderShippingHistory();
        history.setOrder(order);
        history.setShipper(shipper);
        history.setStatus(status);
        history.setNotes(notes);
        history.setImageURL(imageURL);
        history.setTimestamp(LocalDateTime.now());
        shippingHistoryRepository.save(history);

        String oldOrderStatus = order.getOrderStatus();
        String newOrderStatus = oldOrderStatus;

        if ("Delivered".equals(status)) {
            newOrderStatus = "Completed";
            order.setOrderStatus("Completed");
            markCodPaymentAsPaid(orderId);
            notificationService.notifyCustomerAboutOrderStatus(order.getUser().getId(), orderId, "Completed");
        } else if ("Failed_Delivery".equals(status)) {
            newOrderStatus = "Confirmed";
            order.setOrderStatus("Confirmed");
            order.setShipper(null);
            notificationService.notifyCustomerAboutOrderStatus(order.getUser().getId(), orderId, "Confirmed");
        } else {
            order.setOrderStatus("Delivering");
            newOrderStatus = "Delivering";
            notificationService.notifyCustomerAboutOrderStatus(order.getUser().getId(), orderId, status);
        }

        orderRepository.save(order);
        saveOrderHistory(order, oldOrderStatus, newOrderStatus, shipperId, notes);
    }

    private void markCodPaymentAsPaid(Integer orderId) {
        paymentRepository.findByOrder_OrderID(orderId)
                .filter(payment -> payment.getMethod() == PaymentMethod.COD)
                .filter(payment -> payment.getStatus() == PaymentStatus.UNPAID)
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                });
    }

    private ShipperOrderDTO toDto(Order order, Integer shipperId) {
        Payment payment = paymentRepository.findByOrder_OrderID(order.getOrderID()).orElse(null);
        Optional<OrderShippingHistory> latestHistory = getLatestShippingHistory(order.getOrderID(), shipperId);

        ShipperOrderDTO dto = new ShipperOrderDTO();
        dto.setOrderId(order.getOrderID());
        dto.setOrderDate(order.getOrderDate());
        dto.setCustomerName(order.getUser() != null ? order.getUser().getFullName() : null);
        dto.setCustomerPhone(order.getUser() != null ? order.getUser().getPhoneNumber() : null);
        dto.setRecipientName(order.getUser() != null ? order.getUser().getFullName() : null);
        dto.setRecipientPhone(order.getUser() != null ? order.getUser().getPhoneNumber() : null);
        dto.setShippingAddress(OrderPricingUtils.formatAddress(order.getAddress()));
        dto.setGrandTotal(OrderPricingUtils.calculateOrderTotal(order));
        dto.setPaymentMethod(payment != null && payment.getMethod() != null ? payment.getMethod().name() : null);
        dto.setPaymentStatus(payment != null && payment.getStatus() != null ? payment.getStatus().name() : null);
        dto.setOrderStatus(order.getOrderStatus());
        dto.setNotes(order.getNotes());
        dto.setShopName(order.getShop() != null ? order.getShop().getShopName() : null);
        dto.setShopPhone(order.getShop() != null ? order.getShop().getPhoneNumber() : null);
        dto.setShopAddress(order.getShop() != null ? OrderPricingUtils.formatAddress(order.getShop().getAddress()) : null);
        dto.setTotalItems(order.getItems() != null ? order.getItems().size() : 0);
        dto.setCurrentShippingStatus(latestHistory.map(OrderShippingHistory::getStatus).orElse("Assigned"));
        dto.setAssignedAt(latestHistory.map(OrderShippingHistory::getTimestamp).orElse(order.getOrderDate()));
        dto.setLastUpdateTime(latestHistory.map(OrderShippingHistory::getTimestamp).orElse(order.getOrderDate()));
        return dto;
    }

    private Optional<OrderShippingHistory> getLatestShippingHistory(Integer orderId, Integer shipperId) {
        return shippingHistoryRepository.findFirstByOrder_OrderIDAndShipper_IdOrderByTimestampDesc(orderId, shipperId);
    }

    private String getCurrentShippingStatus(Order order, Integer shipperId) {
        return getLatestShippingHistory(order.getOrderID(), shipperId)
                .map(OrderShippingHistory::getStatus)
                .orElse("Assigned");
    }

    private String normalizeFilterStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status) {
            case "Confirmed" -> "Assigned";
            case "Completed" -> "Delivered";
            default -> status;
        };
    }

    private boolean matchesStatus(ShipperOrderDTO dto, String status) {
        if (status == null) {
            return true;
        }
        if ("Delivering".equals(status)) {
            return isInDeliveryFlow(dto.getCurrentShippingStatus());
        }
        return status.equalsIgnoreCase(dto.getCurrentShippingStatus());
    }

    private boolean matchesSearch(ShipperOrderDTO dto, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return true;
        }
        String normalized = searchQuery.trim().toLowerCase();
        return contains(dto.getCustomerName(), normalized)
                || contains(dto.getCustomerPhone(), normalized)
                || contains(dto.getRecipientName(), normalized)
                || contains(dto.getRecipientPhone(), normalized)
                || contains(dto.getShippingAddress(), normalized)
                || String.valueOf(dto.getOrderId()).contains(normalized);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private boolean isInDeliveryFlow(String shippingStatus) {
        return "Picking_Up".equals(shippingStatus)
                || "Delivering".equals(shippingStatus)
                || "Delivery_Attempt".equals(shippingStatus);
    }

    private void saveOrderHistory(Order order, String oldStatus, String newStatus, Integer userId, String notes) {
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedByUser(userRepository.findById(userId).orElse(null));
        history.setNotes(notes);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);
    }
}
