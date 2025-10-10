package com.alotra.entity.order;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import com.alotra.entity.product.ProductVariant;

@Entity
@Table(name = "OrderDetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderDetailID")
    private Integer orderDetailId;

    @ManyToOne
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "VariantID", nullable = false)
    private ProductVariant variant;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "UnitPrice", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "LineDiscount", nullable = false)
    private BigDecimal lineDiscount;

    @Column(name = "LineTotal", nullable = false)
    private BigDecimal lineTotal;
}
