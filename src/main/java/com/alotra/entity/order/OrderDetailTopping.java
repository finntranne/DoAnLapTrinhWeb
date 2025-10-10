package com.alotra.entity.order;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import com.alotra.entity.product.Topping;

@Entity
@Table(name = "OrderDetail_Toppings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailTopping {

    @EmbeddedId
    private OrderDetailToppingId id;

    @ManyToOne
    @MapsId("orderDetailId")
    @JoinColumn(name = "OrderDetailID")
    private OrderDetail orderDetail;

    @ManyToOne
    @MapsId("toppingId")
    @JoinColumn(name = "ToppingID")
    private Topping topping;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "UnitPrice", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "LineTotal", nullable = false)
    private BigDecimal lineTotal;
}

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class OrderDetailToppingId {
    private Integer orderDetailId;
    private Integer toppingId;
}
