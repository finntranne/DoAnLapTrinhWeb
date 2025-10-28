package com.alotra.service.shop.impl;

import com.alotra.dto.shop.ShopRegistrationDTO;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.shop.StoreService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoreServiceImpl implements StoreService {

    // Giả định bạn đã có ShopRepository (được cập nhật ở bước trước)
    @Autowired
    private ShopRepository shopRepository;
    
    @Autowired private UserRepository userRepository;

    @Override
    public List<Shop> findAllActiveShops() {
        return shopRepository.findAllActiveShops();
    }

    @Override
    public String getShopNameById(Integer shopId) {
        if (shopId == null || shopId == 0) {
            return "Tất cả cửa hàng"; // Tên hiển thị khi không chọn shop
        }
        // Gọi hàm đã thêm vào ShopRepository
        return shopRepository.findShopNameByShopId(shopId).orElse("Cửa hàng không tồn tại");
    }

	@Override
	public boolean hasShop(Integer userId) {
		return shopRepository.findByUser_Id(userId).isPresent();
	}
	
	@Override
    @Transactional
    public Shop registerNewShop(Integer userId, ShopRegistrationDTO dto) {
        // 1. Kiểm tra User tồn tại
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng."));

        // 2. Kiểm tra Shop đã tồn tại chưa (theo User)
        if (shopRepository.findByUser_Id(userId).isPresent()) {
            throw new IllegalStateException("Người dùng này đã đăng ký một Shop.");
        }

        // 3. Kiểm tra ShopName đã tồn tại chưa (theo tên)
        if (shopRepository.findByShopName(dto.getShopName()).isPresent()) {
            throw new IllegalStateException("Tên Shop đã tồn tại, vui lòng chọn tên khác.");
        }

        // 4. Tạo Shop Entity
        Shop newShop = new Shop();
        newShop.setUser(user);
        newShop.setShopName(dto.getShopName());
        newShop.setDescription(dto.getDescription());
        newShop.setAddress(dto.getAddress());
        newShop.setPhoneNumber(dto.getPhoneNumber());
        newShop.setStatus((byte) 0); // Đặt trạng thái là Pending (0)

        // 5. Lưu và trả về
        return shopRepository.save(newShop);
    }
}
