package com.alotra.service.vendor;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.topping.ToppingRequestDTO;
import com.alotra.dto.topping.ToppingStatisticsDTO;
import com.alotra.entity.product.Topping;
import com.alotra.entity.shop.Shop;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.service.cloudinary.CloudinaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorToppingService {

    private final ShopRepository shopRepository;
    private final CloudinaryService cloudinaryService;
    private final ToppingRepository toppingRepository;

    @Transactional(readOnly = true)
    public Page<ToppingStatisticsDTO> getShopToppings(Integer shopId, Byte status, String search,
            org.springframework.data.domain.Pageable pageable) {
        return toppingRepository.findShopToppingsFiltered(shopId, status, search, pageable)
                .map(topping -> new ToppingStatisticsDTO(
                        topping,
                        "Approved",
                        topping.getStatus() != null && topping.getStatus() == 1
                                ? "Dang hoat dong"
                                : "Khong hoat dong"));
    }

    public void requestToppingCreation(Integer shopId, ToppingRequestDTO request, Integer userId) {
        Shop shop = loadShop(shopId);

        Topping topping = new Topping();
        topping.setShop(shop);
        topping.setToppingName(request.getToppingName());
        topping.setPrice(request.getAdditionalPrice());
        topping.setStatus((byte) 1);
        topping.setImageURL(resolveImageUrl(request, userId, null));
        toppingRepository.save(topping);
    }

    @Transactional(readOnly = true)
    public Topping getToppingDetail(Integer shopId, Integer toppingId) {
        Topping topping = toppingRepository.findById(toppingId)
                .orElseThrow(() -> new RuntimeException("Topping not found"));
        if (topping.getShop() == null || !topping.getShop().getShopId().equals(shopId)) {
            throw new RuntimeException("Unauthorized: Topping does not belong to this shop");
        }
        return topping;
    }

    @Transactional(readOnly = true)
    public ToppingRequestDTO convertToppingToDTO(Topping topping) {
        ToppingRequestDTO dto = new ToppingRequestDTO();
        dto.setToppingId(topping.getToppingID());
        dto.setToppingName(topping.getToppingName());
        dto.setAdditionalPrice(topping.getPrice());
        dto.setImageURL(topping.getImageURL());
        return dto;
    }

    public void requestToppingUpdate(Integer shopId, ToppingRequestDTO request, Integer userId) {
        Topping topping = getToppingDetail(shopId, request.getToppingId());

        topping.setToppingName(request.getToppingName());
        topping.setPrice(request.getAdditionalPrice());
        topping.setImageURL(resolveImageUrl(request, userId, topping.getImageURL()));
        toppingRepository.save(topping);
    }

    public void requestToppingDeletion(Integer shopId, Integer toppingId, Integer userId) {
        Topping topping = getToppingDetail(shopId, toppingId);
        topping.setStatus((byte) 0);
        toppingRepository.save(topping);
    }

    private Shop loadShop(Integer shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
    }

    private String resolveImageUrl(ToppingRequestDTO request, Integer userId, String existingImageUrl) {
        if (request.getImageFile() == null || request.getImageFile().isEmpty()) {
            return request.getImageURL() != null && !request.getImageURL().isBlank()
                    ? request.getImageURL()
                    : existingImageUrl;
        }

        try {
            Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(
                    request.getImageFile(),
                    "toppings",
                    userId);
            return uploadResult.get("secure_url");
        } catch (Exception ex) {
            throw new RuntimeException("Khong the upload hinh anh topping", ex);
        }
    }
}
