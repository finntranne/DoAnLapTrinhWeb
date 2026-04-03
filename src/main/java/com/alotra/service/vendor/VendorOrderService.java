package com.alotra.service.vendor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.shop.ShopEmployeeDTO;
import com.alotra.dto.shop.ShopOrderDTO;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderHistory;
import com.alotra.entity.order.Payment;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderHistoryRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.PaymentRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.notification.NotificationService;
import com.alotra.service.order.ShipperOrderService;
import com.alotra.util.OrderPricingUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorOrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ShipperOrderService shipperOrderService;
    private final OrderHistoryRepository orderHistoryRepository;

    @Transactional(readOnly = true)
    public Page<ShopOrderDTO> getShopOrders(Integer shopId, String status, String searchQuery, Pageable pageable) {
        Page<Order> orders = orderRepository.findShopOrdersFiltered(shopId, status, searchQuery, pageable);
        List<ShopOrderDTO> dtos = orders.getContent().stream().map(this::toDto).toList();
        return new PageImpl<>(dtos, pageable, orders.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getOrderStatusCounts(Integer shopId) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("ALL", orderRepository.countByShopId(shopId));
        counts.put("Pending", orderRepository.countByShopIdAndStatus(shopId, "Pending"));
        counts.put("Confirmed", orderRepository.countByShopIdAndStatus(shopId, "Confirmed"));
        counts.put("Delivering", orderRepository.countByShopIdAndStatus(shopId, "Delivering"));
        counts.put("Completed", orderRepository.countByShopIdAndStatus(shopId, "Completed"));
        counts.put("Cancelled", orderRepository.countByShopIdAndStatus(shopId, "Cancelled"));
        return counts;
    }

    @Transactional(readOnly = true)
    public Order getOrderDetail(Integer shopId, Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getShop().getShopId().equals(shopId)) {
            throw new RuntimeException("Unauthorized: Order does not belong to this shop");
        }
        return order;
    }

    public void updateOrderStatus(Integer shopId, Integer orderId, String newStatus, Integer userId) {
        Order order = getOrderDetail(shopId, orderId);
        String oldStatus = order.getOrderStatus();
        order.setOrderStatus(newStatus);
        orderRepository.save(order);
        saveOrderHistory(order, oldStatus, newStatus, userId, "Cap nhat trang thai don hang");
        notificationService.notifyCustomerAboutOrderStatus(order.getUser().getId(), orderId, newStatus);
    }

    public void assignShipperToOrder(Integer shopId, Integer orderId, Integer shipperId, Integer userId) {
        Order order = getOrderDetail(shopId, orderId);
        User shipper = loadActiveShipper(shipperId);

        order.setShipper(shipper);
        order.setOrderStatus("Delivering");
        orderRepository.save(order);

        shipperOrderService.createInitialShippingHistory(orderId, shipperId,
                "Don hang duoc gan cho shipper: " + shipper.getFullName());
        saveOrderHistory(order, "Confirmed", "Delivering", userId,
                "Gan shipper: " + shipper.getFullName());
        notificationService.notifyShipperAboutAssignment(shipper.getId(), orderId,
                OrderPricingUtils.formatAddress(order.getAddress()));
    }

    @Transactional(readOnly = true)
    public List<ShopEmployeeDTO> getAvailableShippers(Integer shopId) {
        return userRepository.findByRoles_RoleName("SHIPPER").stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .map(this::toEmployeeDto)
                .toList();
    }

    public void reassignShipper(Integer shopId, Integer orderId, Integer newShipperId, Integer userId, String reason) {
        Order order = getOrderDetail(shopId, orderId);
        User previousShipper = order.getShipper();
        User newShipper = loadActiveShipper(newShipperId);

        order.setShipper(newShipper);
        orderRepository.save(order);

        shipperOrderService.createInitialShippingHistory(orderId, newShipperId,
                "Don hang duoc gan lai. Ly do: " + reason);
        saveOrderHistory(order, order.getOrderStatus(), order.getOrderStatus(), userId,
                "Thay doi shipper tu "
                        + (previousShipper != null ? previousShipper.getFullName() : "N/A")
                        + " sang " + newShipper.getFullName() + ". Ly do: " + reason);
        notificationService.notifyShipperAboutAssignment(newShipper.getId(), orderId,
                OrderPricingUtils.formatAddress(order.getAddress()));
    }

    private ShopOrderDTO toDto(Order order) {
        Payment payment = paymentRepository.findByOrder_OrderID(order.getOrderID()).orElse(null);

        ShopOrderDTO dto = new ShopOrderDTO();
        dto.setOrderId(order.getOrderID());
        dto.setOrderDate(order.getOrderDate());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setPaymentMethod(payment != null && payment.getMethod() != null ? payment.getMethod().name() : null);
        dto.setPaymentStatus(payment != null && payment.getStatus() != null ? payment.getStatus().name() : null);
        dto.setGrandTotal(OrderPricingUtils.calculateOrderTotal(order));
        dto.setCustomerName(order.getUser() != null ? order.getUser().getFullName() : null);
        dto.setCustomerPhone(order.getUser() != null ? order.getUser().getPhoneNumber() : null);
        dto.setRecipientName(order.getUser() != null ? order.getUser().getFullName() : null);
        dto.setRecipientPhone(order.getUser() != null ? order.getUser().getPhoneNumber() : null);
        dto.setShippingAddress(OrderPricingUtils.formatAddress(order.getAddress()));
        dto.setShipperName(order.getShipper() != null ? order.getShipper().getFullName() : null);
        dto.setTotalItems(order.getItems() != null ? order.getItems().size() : 0);
        return dto;
    }

    private ShopEmployeeDTO toEmployeeDto(User user) {
        ShopEmployeeDTO dto = new ShopEmployeeDTO();
        dto.setEmployeeId(user.getId());
        dto.setUserId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAvatarURL(user.getAvatarURL());
        dto.setStatus("Active");
        user.getRoles().stream()
                .filter(role -> "SHIPPER".equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .ifPresent(role -> {
                    dto.setRoleId(role.getId());
                    dto.setRoleName(role.getRoleName());
                });
        return dto;
    }

    private User loadActiveShipper(Integer shipperId) {
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));
        boolean isShipper = shipper.getRoles().stream()
                .map(Role::getRoleName)
                .anyMatch("SHIPPER"::equalsIgnoreCase);
        if (!isShipper) {
            throw new RuntimeException("User is not a shipper");
        }
        return shipper;
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
