package com.alotra.entity.order;

import com.alotra.entity.product.ProductVariant;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "OrderItems")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "orderItemId")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderItemID")
    private Integer orderItemId;

    // Nhiều OrderItem thuộc về MỘT Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    // Liên kết với biến thể sản phẩm (size)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantID", nullable = false)
    private ProductVariant variant;

    @Column(name = "Quantity", nullable = false)
    private int quantity;

    /**
     * Giá của 1 sản phẩm TẠI THỜI ĐIỂM MUA
     * (Bao gồm giá variant + giá toppings)
     */
    @Column(name = "Price", nullable = false)
    private BigDecimal price;
    
    /**
     * Lưu lại danh sách topping đã chọn (dưới dạng chuỗi)
     * Ví dụ: "Trân châu, Thạch dừa"
     */
    @Column(name = "Toppings")
    private String toppingsSnapshot; // Lưu trữ tên toppings
    
    // TotalPrice (tính toán)
    public BigDecimal getTotalPrice() {
        return price.multiply(new BigDecimal(quantity));
    }
}