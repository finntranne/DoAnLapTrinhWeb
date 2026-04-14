package com.alotra.service.checkout;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alotra.entity.cart.CartItem;
import com.alotra.entity.location.Address;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderHistory;
import com.alotra.entity.order.OrderItem;
import com.alotra.entity.order.Payment;
import com.alotra.enums.PaymentStatus;
import com.alotra.repository.cart.CartItemRepository;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.location.AddressRepository;
import com.alotra.repository.order.OrderHistoryRepository;
import com.alotra.repository.order.OrderItemRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.order.PaymentRepository;
import com.alotra.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StandardCheckoutProcessor extends CheckoutTemplate {
    private static final Logger log = LoggerFactory.getLogger(StandardCheckoutProcessor.class);
    
    // Default system values
    private static final BigDecimal DEFAULT_SHIPPING_FEE = BigDecimal.valueOf(30000);
    private static final String INITIAL_STATUS = "Pending";

    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final NotificationService notificationService;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;

    @Override
    protected void validateBusinessRules(CheckoutContext context) {
        if (context.getItemsToOrder() == null || context.getItemsToOrder().isEmpty()) {
            throw new IllegalArgumentException("Khong co san pham hop le trong gio hang.");
        }
        
        context.setShop(context.getItemsToOrder().get(0).getVariant().getProduct().getShop());
        boolean mixedShop = context.getItemsToOrder().stream()
                .map(item -> item.getVariant().getProduct().getShop().getShopId())
                .anyMatch(shopId -> !shopId.equals(context.getShop().getShopId()));
                
        if (mixedShop) {
            throw new IllegalArgumentException("Don hang hien tai chi ho tro thanh toan cac san pham cung mot cua hang.");
        }
    }

    @Override
    protected void prepareAddress(CheckoutContext context) {
        // Assume OrderAddress is already resolved and passed or we retrieve it
        Address chosenAddress = context.getChosenAddress();
        if (chosenAddress == null) {
            chosenAddress = addressRepository.findById(context.getAddressId())
                    .orElseThrow(() -> new IllegalArgumentException("Dia chi giao hang khong hop le."));
        }
        Address orderAddress = createOrderAddressSnapshot(chosenAddress);
        orderAddress = addressRepository.save(orderAddress);
        context.setOrderAddress(orderAddress);
    }
    
    private Address createOrderAddressSnapshot(Address source) {
        Address snapshot = new Address();
        snapshot.setProvince(source.getProvince());
        snapshot.setDistrict(source.getDistrict());
        snapshot.setWard(source.getWard());
        snapshot.setStreetAddress(source.getStreetAddress());
        snapshot.setIsDefault(Boolean.FALSE);
        return snapshot;
    }

    @Override
    protected void saveOrderAndItems(CheckoutContext context) {
        Order order = new Order();
        order.setUser(context.getUser());
        order.setShop(context.getShop());
        order.setAddress(context.getOrderAddress());
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(INITIAL_STATUS);
        order.setShippingFee(DEFAULT_SHIPPING_FEE);
        order.setNotes(context.getNotes());
        
        order = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : context.getItemsToOrder()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariant(cartItem.getVariant());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);
        order.setItems(orderItems);
        
        context.setOrder(order);
    }

    @Override
    protected void processPayment(CheckoutContext context) {
        Payment payment = new Payment();
        payment.setOrder(context.getOrder());
        payment.setMethod(context.getPaymentMethod());
        payment.setStatus(PaymentStatus.UNPAID);
        paymentRepository.save(payment);
    }

    @Override
    protected void createOrderHistory(CheckoutContext context) {
        OrderHistory history = new OrderHistory();
        history.setOrder(context.getOrder());
        history.setOldStatus(null);
        history.setNewStatus(INITIAL_STATUS);
        history.setChangedByUser(context.getUser());
        history.setTimestamp(LocalDateTime.now());
        history.setNotes("Don hang duoc tao tu he thong checkout");
        orderHistoryRepository.save(history);
    }

    @Override
    protected void notifyVendor(CheckoutContext context) {
        if (context.getShop().getUser() != null) {
            try {
                notificationService.notifyVendorAboutNewOrder(
                        context.getShop().getUser().getId(),
                        context.getOrder().getOrderID(),
                        context.getUser().getFullName()
                );
            } catch (Exception ex) {
                log.warn("Khong gui duoc thong bao cho vendor: {}", ex.getMessage());
            }
        }
    }

    @Override
    protected void clearCart(CheckoutContext context) {
        for (CartItem item : context.getItemsToOrder()) {
            item.getToppings().clear();
            context.getCart().removeItem(item);
            cartItemRepository.delete(item);
        }
        cartRepository.save(context.getCart());
    }
}
