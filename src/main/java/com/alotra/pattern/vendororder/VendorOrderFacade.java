package com.alotra.pattern.vendororder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.alotra.entity.order.Order;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.pattern.vendororder.command.AssignShipperCommand;
import com.alotra.pattern.vendororder.command.CancelOrderCommand;
import com.alotra.pattern.vendororder.command.ConfirmOrderCommand;
import com.alotra.pattern.vendororder.context.VendorOrderContext;
import com.alotra.pattern.vendororder.observer.OrderEventPublisher;
import com.alotra.pattern.vendororder.strategy.ShipperSelectionContext;
import com.alotra.pattern.vendororder.strategy.ShipperSelectionStrategy;
import com.alotra.pattern.vendororder.strategy.ShipperSelectionStrategyFactory;
import com.alotra.pattern.vendororder.strategy.ShipperSelectionType;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorOrderFacade {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ConfirmOrderCommand confirmOrderCommand;
    private final AssignShipperCommand assignShipperCommand;
    private final CancelOrderCommand cancelOrderCommand;
    private final OrderEventPublisher orderEventPublisher;
    private final ShipperSelectionStrategyFactory shipperSelectionStrategyFactory;

    public void confirmOrder(Integer shopId, Integer orderId, Integer actorId) {
        Order order = loadOrder(shopId, orderId);
        User actor = loadUser(actorId);
        VendorOrderContext ctx = new VendorOrderContext(order, actor, null, order.getShop(),
                "Cap nhat trang thai don hang", orderEventPublisher);
        confirmOrderCommand.execute(ctx);
        orderRepository.save(order);
    }

    public void assignShipper(Integer shopId, Integer orderId, Integer shipperId, Integer actorId, String note) {
        assignShipper(shopId, orderId, shipperId, actorId, note, ShipperSelectionType.MANUAL);
    }

    public void assignShipper(Integer shopId, Integer orderId, Integer shipperId, Integer actorId, String note,
            ShipperSelectionType selectionType) {
        Order order = loadOrder(shopId, orderId);
        User actor = loadUser(actorId);
        User shipper = selectShipper(order, shipperId, selectionType);
        VendorOrderContext ctx = new VendorOrderContext(order, actor, shipper, order.getShop(),
                note, orderEventPublisher);
        assignShipperCommand.execute(ctx);
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public User recommendShipper(Integer shopId, Integer orderId, ShipperSelectionType selectionType) {
        Order order = loadOrder(shopId, orderId);
        return selectShipper(order, null, selectionType);
    }

    public void cancelOrder(Integer shopId, Integer orderId, Integer actorId, String note) {
        Order order = loadOrder(shopId, orderId);
        User actor = loadUser(actorId);
        VendorOrderContext ctx = new VendorOrderContext(order, actor, null, order.getShop(),
                note, orderEventPublisher);
        cancelOrderCommand.execute(ctx);
        orderRepository.save(order);
    }

    private Order loadOrder(Integer shopId, Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Shop shop = order.getShop();
        if (shop == null || !shopId.equals(shop.getShopId())) {
            throw new RuntimeException("Unauthorized: Order does not belong to this shop");
        }
        return order;
    }

    private User loadUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private User selectShipper(Order order, Integer preferredShipperId, ShipperSelectionType selectionType) {
        List<User> candidates = userRepository.findByRoles_RoleName("SHIPPER").stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .filter(user -> order.getShipper() == null || !user.getId().equals(order.getShipper().getId()))
                .toList();

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No active shipper available");
        }

        ShipperSelectionStrategy strategy = shipperSelectionStrategyFactory.getStrategy(selectionType);
        return strategy.select(new ShipperSelectionContext(order, preferredShipperId, candidates));
    }
}
