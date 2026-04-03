
package com.alotra.entity.order; // Giữ package này

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List; // Sử dụng List thay vì Set cho orderDetails

import com.alotra.entity.location.Address;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;

import jakarta.persistence.*; 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

@Entity
@Table(name = "Orders")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID") 
    private Integer orderID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false) 
    private User user; // Khách hàng

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopID", nullable = false) 
    private Shop shop; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AddressID", nullable = false)
    private Address address;

    @Column(name = "OrderDate", nullable = false, updatable = false) 
    private LocalDateTime orderDate;

    @Column(name = "OrderStatus", nullable = false, length = 30) 
    private String orderStatus; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShipperID") 
    private User shipper; 

    @Column(name = "ShippingFee", nullable = false, precision = 12, scale = 2) 
    private BigDecimal shippingFee = BigDecimal.ZERO; 

    @Column(name = "Notes", length = 500, columnDefinition = "NVARCHAR(500)") 
    private String notes; 

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) 
    private List<OrderItem> items = new ArrayList<>();

   
}