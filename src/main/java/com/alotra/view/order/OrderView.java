package com.alotra.view.order;

import java.math.BigDecimal;
import java.util.List;

import com.alotra.entity.location.Address;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.Payment;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.enums.PaymentMethod;
import com.alotra.enums.PaymentStatus;
import com.alotra.util.OrderPricingUtils;

public class OrderView {

    private final Order order;
    private final List<OrderLineView> orderDetails;
    private final BigDecimal subtotal;
    private final BigDecimal discountAmount;
    private final BigDecimal grandTotal;
    private final String paymentMethod;
    private final String paymentStatus;
    private final String recipientName;
    private final String recipientPhone;
    private final String shippingAddress;

    private OrderView(Order order, Payment payment) {
        this.order = order;
        this.orderDetails = order.getItems().stream().map(OrderLineView::new).toList();
        this.subtotal = orderDetails.stream()
                .map(OrderLineView::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.discountAmount = BigDecimal.ZERO;
        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        this.grandTotal = subtotal.add(shippingFee);
        this.paymentMethod = formatPaymentMethod(payment != null ? payment.getMethod() : null);
        this.paymentStatus = formatPaymentStatus(payment != null ? payment.getStatus() : null);
        this.recipientName = order.getUser() != null ? order.getUser().getFullName() : "";
        this.recipientPhone = order.getUser() != null ? order.getUser().getPhoneNumber() : "";
        this.shippingAddress = OrderPricingUtils.formatAddress(order.getAddress());
    }

    public static OrderView from(Order order, Payment payment) {
        return new OrderView(order, payment);
    }

    public Integer getOrderID() {
        return order.getOrderID();
    }

    public java.time.LocalDateTime getOrderDate() {
        return order.getOrderDate();
    }

    public java.time.LocalDateTime getCompletedAt() {
        // Current order model does not persist a dedicated completion timestamp.
        return order.getOrderDate();
    }

    public String getOrderStatus() {
        return order.getOrderStatus();
    }

    public User getUser() {
        return order.getUser();
    }

    public Shop getShop() {
        return order.getShop();
    }

    public Address getAddress() {
        return order.getAddress();
    }

    public User getShipper() {
        return order.getShipper();
    }

    public BigDecimal getShippingFee() {
        return order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
    }

    public String getNotes() {
        return order.getNotes();
    }

    public String getCancellationReason() {
        return null;
    }

    public List<OrderLineView> getOrderDetails() {
        return orderDetails;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public Order getSource() {
        return order;
    }

    private static String formatPaymentMethod(PaymentMethod method) {
        if (method == null) {
            return "Unknown";
        }

        return switch (method) {
            case COD -> "Cash";
            case VNPAY -> "VNPay";
            case BANK_TRANSFER -> "VietQR";
            case MOMO -> "MOMO";
            case ZALOPAY -> "ZALOPAY";
        };
    }

    private static String formatPaymentStatus(PaymentStatus status) {
        if (status == null) {
            return "Unpaid";
        }

        return switch (status) {
            case PAID -> "Paid";
            case REFUNDED -> "Refunded";
            case UNPAID -> "Unpaid";
        };
    }
}
