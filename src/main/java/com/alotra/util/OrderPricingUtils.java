package com.alotra.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.StringJoiner;

import com.alotra.entity.location.Address;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderItem;
import com.alotra.entity.product.Topping;

public final class OrderPricingUtils {

    private OrderPricingUtils() {
    }

    public static BigDecimal calculateOrderSubtotal(Order order) {
        return calculateOrderSubtotal(order != null ? order.getItems() : List.of());
    }

    public static BigDecimal calculateOrderSubtotal(List<OrderItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (items == null) {
            return subtotal;
        }

        for (OrderItem item : items) {
            subtotal = subtotal.add(calculateLineTotal(item));
        }

        return subtotal;
    }

    public static BigDecimal calculateLineTotal(OrderItem item) {
        if (item == null || item.getVariant() == null || item.getVariant().getPrice() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal unitPrice = item.getVariant().getPrice();
        if (item.getToppings() != null) {
            for (Topping topping : item.getToppings()) {
                if (topping != null && topping.getPrice() != null) {
                    unitPrice = unitPrice.add(topping.getPrice());
                }
            }
        }

        return unitPrice
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateOrderTotal(Order order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        return calculateOrderSubtotal(order).add(shippingFee).setScale(2, RoundingMode.HALF_UP);
    }

    public static String formatAddress(Address address) {
        if (address == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(", ");
        addIfPresent(joiner, address.getStreetAddress());
        addIfPresent(joiner, address.getWard());
        addIfPresent(joiner, address.getDistrict());
        addIfPresent(joiner, address.getProvince());
        return joiner.toString();
    }

    private static void addIfPresent(StringJoiner joiner, String value) {
        if (value != null && !value.isBlank()) {
            joiner.add(value.trim());
        }
    }
}
