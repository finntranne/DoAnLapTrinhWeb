package com.alotra.service.cart.impl; // Giữ package này

import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Topping;
import com.alotra.entity.promotion.Promotion;
import com.alotra.entity.user.User;
import com.alotra.repository.cart.CartItemRepository;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.product.ProductVariantRepository;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.service.cart.CartService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException; // Cần import này
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.stream.Collectors; // Import Collectors

// Import cho logic giảm giá
import com.alotra.service.product.ProductService;
import com.alotra.model.ProductSaleDTO;
import java.math.RoundingMode;

@Service
@Slf4j
public class CartServiceImpl implements CartService { // Đảm bảo CartService dùng User

    @Autowired
	CartRepository cartRepository;
	@Autowired
	CartItemRepository cartItemRepository;
	@Autowired
	ProductVariantRepository variantRepository;
	@Autowired
	ToppingRepository toppingRepository;
	
	// *** THÊM: Inject ProductService để lấy thông tin giảm giá ***
	@Autowired
    private ProductService productService; 
	
    // *** Giữ nguyên: addItemToCart ***
    @Override
    @Transactional
    public CartItem addItemToCart(User user, Integer variantId, int quantity, List<Integer> toppingIds) {
        // 1. Kiểm tra đầu vào
        if (user == null) throw new IllegalArgumentException("Người dùng không hợp lệ.");
        if (quantity <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");

        // 2. Tìm biến thể sản phẩm (dùng Integer ID)
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Size sản phẩm với ID: " + variantId));

        // 3. Tìm hoặc tạo giỏ hàng cho User (dùng User ID)
        Cart cart = cartRepository.findByUser_Id(user.getId()) // *** SỬA: findByUser_Id ***
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user); // *** SỬA: setUser ***
                    return cartRepository.save(newCart);
                });

        // 4. Lấy danh sách Topping đã chọn (dùng Integer ID)
        final Set<Topping> finalSelectedToppings;
        if (toppingIds != null && !toppingIds.isEmpty()) {
            finalSelectedToppings = new HashSet<>(toppingRepository.findAllById(toppingIds));
            if (finalSelectedToppings.size() != toppingIds.size()) {
                System.err.println("Cảnh báo: Một vài ID topping không hợp lệ: " + toppingIds);
            }
        } else {
            finalSelectedToppings = Collections.emptySet();
        }

        // 5. KIỂM TRA MÓN HÀNG TỒN TẠI
        List<CartItem> currentItems = cartItemRepository.findByCart_CartID(cart.getCartID());

        Optional<CartItem> existingItemOpt = currentItems.stream()
                .filter(item ->
                    item.getVariant().getVariantID().equals(variantId) &&
                    Objects.equals(item.getSelectedToppings().stream().map(Topping::getToppingID).collect(Collectors.toSet()),
                                   finalSelectedToppings.stream().map(Topping::getToppingID).collect(Collectors.toSet()))
                )
                .findFirst();

        if (existingItemOpt.isPresent()) {
            // 6a. Nếu có rồi -> Tăng số lượng
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            return cartItemRepository.save(existingItem);
        } else {
            // 6b. Nếu chưa có -> Tạo CartItem mới
            CartItem newItem = new CartItem();
            newItem.setVariant(variant);
            newItem.setQuantity(quantity);
            newItem.setSelectedToppings(finalSelectedToppings);
            newItem.setCart(cart); 
            return cartItemRepository.save(newItem);
        }
    }
    
    // *** Giữ nguyên: removeItemFromCart ***
    @Override
    @Transactional
    public void removeItemFromCart(User user, Integer cartItemId) {
        if (user == null) throw new IllegalArgumentException("Người dùng không hợp lệ.");
        if (cartItemId == null) throw new IllegalArgumentException("ID món hàng không hợp lệ.");

        Cart cart = cartRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng cho người dùng."));

        CartItem itemToRemove = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));

        if (!itemToRemove.getCart().getCartID().equals(cart.getCartID())) {
            throw new AccessDeniedException("Bạn không có quyền xóa món hàng này.");
        }
        cartItemRepository.delete(itemToRemove);
    }

    // *** Giữ nguyên: updateItemQuantity ***
    @Override
    @Transactional
    public CartItem updateItemQuantity(User user, Integer cartItemId, int newQuantity) {
        if (user == null) throw new IllegalArgumentException("Người dùng không hợp lệ.");
        if (cartItemId == null) throw new IllegalArgumentException("ID món hàng không hợp lệ.");
        if (newQuantity <= 0) {
            removeItemFromCart(user, cartItemId); 
            return null; 
        }

        CartItem itemToUpdate = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));

        if (!itemToUpdate.getCart().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật món hàng này.");
        }

        itemToUpdate.setQuantity(newQuantity);
        CartItem updatedItem = cartItemRepository.save(itemToUpdate);
        System.out.println("Updated CartItem ID: " + cartItemId + " to quantity: " + newQuantity); // Log
        return updatedItem;
    }

    // *** Giữ nguyên: getCartItemCount ***
    @Override
    public int getCartItemCount(User user) {
        if (user == null) return 0;

        Optional<Cart> cartOpt = cartRepository.findByUser_Id(user.getId()); 

        if (cartOpt.isPresent()) {
            List<CartItem> items = cartItemRepository.findByCart_CartID(cartOpt.get().getCartID());
            return items.stream()
                       .mapToInt(CartItem::getQuantity)
                       .sum();
        } else {
            return 0;
        }
    }

    // *** CẬP NHẬT: getSubtotal (để sử dụng logic giảm giá) ***
    @Override
    public BigDecimal getSubtotal(User user) {
        if (user == null) return BigDecimal.ZERO;

        Optional<Cart> cartOpt = cartRepository.findByUser_Id(user.getId());
        if (cartOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<CartItem> items = cartItemRepository.findByCartIdWithDetails(cartOpt.get().getCartID());
        if (items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 3. Tính tổng tiền (SỬ DỤNG HÀM getLineTotal MỚI)
        return items.stream()
                   .map(this::getLineTotal) // <-- THAY ĐỔI QUAN TRỌNG
                   .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // *** Giữ nguyên: clearCart ***
    @Override
    @Transactional
    public void clearCart(User user) {
        if (user == null) return;

        Optional<Cart> cartOpt = cartRepository.findByUser_Id(user.getId());
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cartItemRepository.deleteByCart_CartID(cart.getCartID()); 
            System.out.println("Cleared cart for User ID: " + user.getId());
        }
    }

    // *** CẬP NHẬT: calculateSubtotal (để sử dụng logic giảm giá) ***
    @Override
    public BigDecimal calculateSubtotal(Set<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // SỬ DỤNG HÀM getLineTotal MỚI
        return items.stream()
                   .map(this::getLineTotal) // <-- THAY ĐỔI QUAN TRỌNG
                   .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // *** HÀM MỚI: Tính giá 1 dòng (bao gồm giảm giá, topping) ***
    // (Bạn cũng nên thêm hàm này vào interface CartService.java)
    @Override
    public BigDecimal getLineTotal(CartItem item) {
        if (item == null || item.getVariant() == null) {
            return BigDecimal.ZERO;
        }

        ProductVariant variant = item.getVariant();
        BigDecimal basePrice = variant.getPrice(); // Giá gốc theo size

        // Lấy % giảm giá từ ProductService
        Integer discountPercent = null;
        if (variant.getProduct() != null && variant.getProduct().getProductID() != null) {
        	Integer defaultShopId = 0;
        	Optional<ProductSaleDTO> saleDTOOpt = productService.findProductSaleDataById(
                    variant.getProduct().getProductID(),
                    defaultShopId // <-- THAM SỐ THIẾU ĐÃ ĐƯỢC THÊM
                );
            
            if (saleDTOOpt.isPresent()) {
                discountPercent = saleDTOOpt.get().getDiscountPercentage(); // Lấy % giảm giá
            }
        }

        BigDecimal discountedPrice; // Giá 1 cái (đã giảm, chưa topping)
        if (discountPercent != null && discountPercent > 0) {
            // Tính giá sau khi giảm
            BigDecimal discountMultiplier = new BigDecimal(100 - discountPercent)
                                                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            discountedPrice = basePrice.multiply(discountMultiplier);
        } else {
            // Không giảm giá
            discountedPrice = basePrice;
        }

        // Cộng giá toppings (topping không giảm giá)
        if (item.getSelectedToppings() != null) {
            for (Topping topping : item.getSelectedToppings()) {
                if (topping != null && topping.getAdditionalPrice() != null) {
                    discountedPrice = discountedPrice.add(topping.getAdditionalPrice());
                }
            }
        }

        // Nhân với số lượng để ra tổng tiền của dòng
        return discountedPrice.multiply(new BigDecimal(item.getQuantity()));
    }
    
    @Override
    public BigDecimal calculateDiscountAmount(BigDecimal subtotal, Promotion promotion) {
        if (promotion == null || promotion.getDiscountValue() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount = BigDecimal.ZERO;

        // 1. Xử lý trường hợp PERCENTAGE (theo dữ liệu của bạn)
        if ("PERCENTAGE".equalsIgnoreCase(promotion.getDiscountType())) {
            
            // Tính giá trị giảm theo %: subtotal * (DiscountValue / 100)
            BigDecimal discountRate = promotion.getDiscountValue().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            discountAmount = subtotal.multiply(discountRate);
            
            // 2. Kiểm tra MaxDiscountAmount
            if (promotion.getMaxDiscountAmount() != null && discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discountAmount = promotion.getMaxDiscountAmount();
            }
            
        } 
        // 3. Xử lý trường hợp FixedAmount (Nếu bạn có type này)
        else if ("FIXED".equalsIgnoreCase(promotion.getDiscountType())) {
            discountAmount = promotion.getDiscountValue();
        }
        
        // 4. Trả về giá trị đã làm tròn
        // Sử dụng RoundingMode.DOWN hoặc HALF_UP tùy chính sách, nhưng phải về số nguyên (Scale 0)
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }
}