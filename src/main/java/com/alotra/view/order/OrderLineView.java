package com.alotra.view.order;

import java.math.BigDecimal;
import java.util.List;

import com.alotra.entity.order.OrderItem;
import com.alotra.entity.product.Topping;
import com.alotra.entity.product.ProductVariant;
import com.alotra.util.OrderPricingUtils;

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
        this.subtotal = OrderPricingUtils.calculateLineTotal(orderItem);
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

    public List<OrderLineToppingView> getToppings() {
        if (orderItem == null || orderItem.getToppings() == null || orderItem.getToppings().isEmpty()) {
            return List.of();
        }
        return orderItem.getToppings().stream()
                .map(OrderLineToppingView::new)
                .toList();
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public static class OrderLineToppingView {
        private final Topping topping;

        public OrderLineToppingView(Topping topping) {
            this.topping = topping;
        }

        public Topping getTopping() {
            return topping;
        }
    }
}
