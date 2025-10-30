
package com.alotra.entity.product; // Giữ package này

import java.math.BigDecimal;

import jakarta.persistence.*; // Import đầy đủ JPA annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // Import Exclude
import lombok.NoArgsConstructor;
import lombok.ToString; // Import Exclude

@Entity
@Table(name = "ProductVariants", // Khớp DB
    uniqueConstraints = @UniqueConstraint( // Giữ lại constraint từ nhánh lam, khớp DB
        name = "UQ_ProductVariant_Unique",
        columnNames = {"ProductID", "SizeID"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
// Thêm Excludes
@ToString(exclude = {"product", "size"})
@EqualsAndHashCode(exclude = {"product", "size"})
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VariantID") // Khớp DB và nhánh lam
    private Integer variantID; // Giữ tên theo nhánh lam

    @ManyToOne(fetch = FetchType.LAZY) // Giữ LAZY fetch
    @JoinColumn(name = "ProductID", nullable = false) // Khớp DB
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY) // Giữ LAZY fetch
    @JoinColumn(name = "SizeID", nullable = false) // Khớp DB
    private Size size;

    @Column(name = "Price", nullable = false, precision = 10, scale = 2) // Khớp DB và nhánh lam
    private BigDecimal price;

    @Column(name = "Stock", nullable = false) // Khớp DB và nhánh lam
    private Integer stock = 0; // Giữ kiểu Integer và giá trị mặc định từ nhánh lam

    @Column(name = "SKU", unique = true, length = 50) // Giữ lại từ nhánh lam, khớp DB
    private String sku;

    // Bỏ @OneToMany Set<OrderDetail> orderDetails từ HEAD vì nó sai logic (OrderDetail phải trỏ về ProductVariant)
}