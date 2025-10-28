package com.alotra.service.shop;

import com.alotra.dto.shop.ShopRegistrationDTO;
import com.alotra.entity.shop.Shop;
import java.util.List;

// Giữ tên StoreService nếu bạn muốn, nhưng ShopService sẽ nhất quán hơn với Entity
public interface StoreService { 
    
    /**
     * Lấy danh sách tất cả các cửa hàng đang hoạt động (dùng cho dropdown)
     */
    List<Shop> findAllActiveShops();

    /**
     * Lấy tên cửa hàng dựa trên ID. Trả về tên mặc định nếu ID = 0.
     */
    String getShopNameById(Integer shopId);
    
    boolean hasShop(Integer userId);
    
    Shop registerNewShop(Integer userId, ShopRegistrationDTO dto);
}