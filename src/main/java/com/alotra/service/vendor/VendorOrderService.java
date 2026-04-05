package com.alotra.service.vendor;

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
import com.alotra.entity.order.Payment;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.PaymentRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.util.OrderPricingUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorOrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

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

    @Transactional(readOnly = true)
    public List<ShopEmployeeDTO> getAvailableShippers(Integer shopId) {
        return userRepository.findByRoles_RoleName("SHIPPER").stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .map(this::toEmployeeDto)
                .toList();
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

}
