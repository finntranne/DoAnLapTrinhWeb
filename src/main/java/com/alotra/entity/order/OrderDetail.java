package com.alotra.entity.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.alotra.entity.product.ProductVariant;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "OrderDetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"order", "variant"})
public class OrderDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderDetailID")
    private Integer orderDetailID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantID", nullable = false)
    private ProductVariant variant;
    
    @Column(name = "Quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "UnitPrice", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "Subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    
    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL)
    private List<OrderDetailTopping> toppings = new ArrayList<>();
}
