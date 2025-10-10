package com.alotra.entity.cart;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import com.alotra.entity.product.Topping;

@Entity
@Table(name = "CartItem_Toppings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemTopping {

    @EmbeddedId
    private CartItemToppingId id;

    @ManyToOne
    @MapsId("cartItemId")
    @JoinColumn(name = "CartItemID")
    private CartItem cartItem;

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
class CartItemToppingId {
    private Integer cartItemId;
    private Integer toppingId;
}
