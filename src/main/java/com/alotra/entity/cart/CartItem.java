
package com.alotra.entity.cart; // Giữ package này

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet; // Import HashSet
import java.util.List;
import java.util.Set;    // Import Set

import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Topping; // Import Topping

import jakarta.persistence.*; // Import các annotation cần thiết
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

@Entity
@Table(name = "CartItems") // Khớp DB
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CartItemID") 
    private Integer cartItemID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CartID", nullable = false)
    @EqualsAndHashCode.Exclude 
    @ToString.Exclude 
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantID", nullable = false)
    @EqualsAndHashCode.Exclude 
    @ToString.Exclude 
    private ProductVariant variant;
    
    @ManyToMany
    @JoinTable(
            name = "CartItemToppings",
            joinColumns = @JoinColumn(name = "CartItemID"),
            inverseJoinColumns = @JoinColumn(name = "ToppingID")
    )
    private List<Topping> toppings = new ArrayList<>();

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

}