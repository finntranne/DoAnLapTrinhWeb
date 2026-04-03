

package com.alotra.entity.cart; // Giữ package này

import java.time.LocalDateTime;
import java.util.HashSet; // Sử dụng Set từ HEAD
import java.util.Set; // Sử dụng Set từ HEAD

import com.alotra.entity.user.User; // Sử dụng User từ nhánh lam

import jakarta.persistence.*; // Import các annotation cần thiết
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Cần cho @Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Cần cho @Exclude

@Entity
@Table(name = "Carts") // Tên bảng khớp DB
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CartID")
    private Integer cartID;

    
    @OneToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "UserID", nullable = false, unique = true)
    @EqualsAndHashCode.Exclude 
    @ToString.Exclude 
    private User user;

    
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude 
    @ToString.Exclude 
    private Set<CartItem> items = new HashSet<>();

   
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

  
}
