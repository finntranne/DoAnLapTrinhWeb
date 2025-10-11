package com.alotra;

import com.alotra.model.ProductSaleDTO;
import com.alotra.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DebugRunner implements CommandLineRunner {

    private final ProductService productService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("\n=================================================");
        System.out.println("======= DEBUG: KIỂM TRA DỮ LIỆU TOP PRODUCTS =======");
        System.out.println("=================================================\n");

        try {
            // Gọi trực tiếp phương thức service để kiểm tra
            List<ProductSaleDTO> topProducts = productService.getTopProducts();

            if (topProducts.isEmpty()) {
                System.out.println("!!! KẾT QUẢ: KHÔNG TÌM THẤY SẢN PHẨM BÁN CHẠY NÀO.");
                System.out.println("=> Gợi ý: Kiểm tra lại dữ liệu trong CSDL. Đảm bảo có đơn hàng 'Completed' và có sản phẩm được bán.");
            } else {
                System.out.println(">>> THÀNH CÔNG! ĐÃ TÌM THẤY " + topProducts.size() + " SẢN PHẨM BÁN CHẠY:");
                // Lặp qua từng sản phẩm và in ra
                for (ProductSaleDTO sale : topProducts) {
                    System.out.println(
                        "    - Sản phẩm: " + sale.getProduct().getProductName() +
                        " | Đã bán: " + sale.getTotalSold()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("\nXXX ĐÃ XẢY RA LỖI KHI TRUY VẤN DATABASE XXX");
            e.printStackTrace();
        }

        System.out.println("\n=================================================");
        System.out.println("======= KẾT THÚC DEBUG =======");
        System.out.println("=================================================\n");
    }
}