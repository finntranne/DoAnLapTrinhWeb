package com.alotra.view.order;

import java.math.BigDecimal;
import java.util.List;

import com.alotra.entity.order.OrderItem;
import com.alotra.entity.product.ProductVariant;

public class OrderLineView {

    private final OrderItem orderItem;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    public OrderLineView(OrderItem orderItem) {
        this.orderItem = orderItem;
        this.unitPrice = orderItem != null
                && orderItem.getVariant() != null
                && orderItem.getVariant().getPrice() != null
                        ? orderItem.getVariant().getPrice()
                        : BigDecimal.ZERO;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(orderItem != null ? orderItem.getQuantity() : 0));
    }

    public Integer getOrderDetailID() {
        return orderItem != null ? orderItem.getOrderItemId() : null;
    }

    public Integer getOrderItemId() {
        return orderItem != null ? orderItem.getOrderItemId() : null;
    }

    public ProductVariant getVariant() {
        return orderItem != null ? orderItem.getVariant() : null;
    }

    public Integer getQuantity() {
        return orderItem != null ? orderItem.getQuantity() : 0;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public List<Object> getToppings() {
        return List.of();
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }
}
