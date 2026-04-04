package com.alotra.entity.order;

import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Topping;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "OrderItems")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"orderItemId", "toppings", "variant"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderItemID")
    private Integer orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantID", nullable = false)
    private ProductVariant variant;

    @Column(name = "Quantity", nullable = false)
    private int quantity;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "OrderItemToppings",
        joinColumns = @JoinColumn(name = "OrderItemID"),
        inverseJoinColumns = @JoinColumn(name = "ToppingID")
    )
    private Set<Topping> toppings = new HashSet<>();

}