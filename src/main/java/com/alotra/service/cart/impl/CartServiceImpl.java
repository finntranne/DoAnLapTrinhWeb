package com.alotra.service.cart.impl;

import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Topping; // Import
import com.alotra.entity.user.Customer;
import com.alotra.repository.cart.CartItemRepository;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.product.ProductVariantRepository;
import com.alotra.repository.product.ToppingRepository; // Import
import com.alotra.service.cart.CartService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.List;    // Import
import java.util.Objects;
import java.util.Set;     // Import
import java.util.HashSet; // Import
import java.util.Optional;// Import
import java.math.BigDecimal;
import java.util.Collections;

@Service
public class CartServiceImpl implements CartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private ToppingRepository toppingRepository; // Tiêm Topping Repo

    @Override
    @Transactional // Đảm bảo tất cả thao tác thành công hoặc không gì cả
    public void addItemToCart(Customer customer, Integer variantId, int quantity, List<Integer> toppingIds) {
        // 1. Kiểm tra đầu vào
        if (customer == null) throw new IllegalArgumentException("Khách hàng không hợp lệ.");
        if (quantity <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");

        // 2. Tìm biến thể sản phẩm (size + giá gốc)
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Size sản phẩm với ID: " + variantId));

        // 3. Tìm hoặc tạo giỏ hàng cho khách
        Cart cart = cartRepository.findByCustomer(customer)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomer(customer);
                    return cartRepository.save(newCart); // Lưu giỏ hàng mới trước
                });

     // 4. Lấy danh sách Topping đã chọn (nếu có) - Khai báo là final
        final Set<Topping> finalSelectedToppings;
        if (toppingIds != null && !toppingIds.isEmpty()) {
            finalSelectedToppings = new HashSet<>(toppingRepository.findAllById(toppingIds)); // Gán giá trị
            // Kiểm tra xem có tìm thấy đủ topping không
            if (finalSelectedToppings.size() != toppingIds.size()) {
                System.err.println("Cảnh báo: Một vài ID topping không hợp lệ: " + toppingIds);
                // Có thể bạn muốn ném ra lỗi ở đây nếu topping không hợp lệ là nghiêm trọng
                // throw new EntityNotFoundException("Một vài topping không hợp lệ.");
            }
        } else {
            finalSelectedToppings = Collections.emptySet(); // Gán giá trị là một Set rỗng
        }
        // =======================

     // === 5. KIỂM TRA MÓN HÀNG TỒN TẠI (Đảm bảo so sánh Set chính xác) ===
        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item ->
                    // Điều kiện 1: Cùng Variant (Size)
                    item.getVariant().equals(variant) &&
                    // Điều kiện 2: Cùng Set Topping (Set.equals() hoạt động tốt khi các phần tử có equals/hashCode đúng)
                    Objects.equals(item.getSelectedToppings(), finalSelectedToppings)
                    // Hoặc bạn có thể tự viết hàm so sánh Set nếu cần độ chính xác cao hơn
                    // compareToppingSets(item.getSelectedToppings(), finalSelectedToppings)
                )
                .findFirst();

        if (existingItemOpt.isPresent()) {
            // 6a. Nếu có rồi -> Tăng số lượng
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            cartItemRepository.save(existingItem);
        } else {
            // 6b. Nếu chưa có -> Tạo CartItem mới
            CartItem newItem = new CartItem();
            newItem.setVariant(variant);
            newItem.setQuantity(quantity);
            newItem.setSelectedToppings(finalSelectedToppings); // <-- Dùng biến final
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }
    }
    
    @Override
    public BigDecimal calculateSubtotal(Set<CartItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        if (items == null) {
            return total;
        }
        for (CartItem item : items) {
            BigDecimal itemPrice = item.getVariant().getPrice();
            for (com.alotra.entity.product.Topping topping : item.getSelectedToppings()) {
                itemPrice = itemPrice.add(topping.getAdditionalPrice());
            }
            total = total.add(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }
    
    @Override
    @Transactional
    public void removeItemFromCart(Customer customer, Long cartItemId) {
        if (customer == null) {
            throw new IllegalArgumentException("Khách hàng không hợp lệ.");
        }
        if (cartItemId == null) {
            throw new IllegalArgumentException("ID món hàng không hợp lệ.");
        }

        // 1. Tìm giỏ hàng của khách
        Cart cart = cartRepository.findByCustomer(customer)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng cho khách hàng."));

        // 2. Tìm món hàng cần xóa trong database
        CartItem itemToRemove = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));

        // 3. **Kiểm tra bảo mật:** Đảm bảo món hàng này thuộc về đúng giỏ hàng của khách hiện tại
        if (!itemToRemove.getCart().equals(cart)) {
            // Ném lỗi nếu người dùng cố xóa món hàng không thuộc giỏ của họ
            throw new AccessDeniedException("Bạn không có quyền xóa món hàng này.");
        }

        // 4. Xóa món hàng
        // Cách 1: Dùng trực tiếp repository (phổ biến)
        cartItemRepository.delete(itemToRemove);
    }
    
    @Override
    @Transactional
    public CartItem updateItemQuantity(Customer customer, Long cartItemId, int newQuantity) {
        if (customer == null) {
            throw new IllegalArgumentException("Khách hàng không hợp lệ.");
        }
        if (cartItemId == null) {
            throw new IllegalArgumentException("ID món hàng không hợp lệ.");
        }
        if (newQuantity <= 0) {
            // Hoặc bạn có thể tự động xóa nếu số lượng <= 0
            // removeItemFromCart(customer, cartItemId);
            // return null; // Hoặc ném lỗi tùy logic mong muốn
             throw new IllegalArgumentException("Số lượng mới phải lớn hơn 0.");
        }
        // Có thể thêm kiểm tra số lượng tối đa nếu muốn

        // 1. Tìm giỏ hàng của khách
        Cart cart = cartRepository.findByCustomer(customer)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng cho khách hàng."));

        // 2. Tìm món hàng cần cập nhật
        CartItem itemToUpdate = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));

        // 3. Kiểm tra bảo mật: Đảm bảo món hàng thuộc về giỏ của khách hiện tại
        if (!itemToUpdate.getCart().equals(cart)) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật món hàng này.");
        }

        // 4. Cập nhật số lượng
        itemToUpdate.setQuantity(newQuantity);

        // 5. Lưu lại thay đổi và trả về item đã cập nhật
        CartItem updatedItem = cartItemRepository.save(itemToUpdate);
        System.out.println("Updated CartItem ID: " + cartItemId + " to quantity: " + newQuantity); // Log
        return updatedItem;
    }
    
    @Override
    public int getCartItemCount(Customer customer) {
        if (customer == null) {
            return 0; // Chưa đăng nhập hoặc không có customer thì số lượng là 0
        }
        // Tìm giỏ hàng của khách
        Optional<Cart> cartOpt = cartRepository.findByCustomer(customer);

        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            // Tính tổng quantity của tất cả CartItem trong giỏ
            return cart.getItems().stream()
                       .mapToInt(CartItem::getQuantity) // Lấy quantity của mỗi item
                       .sum(); // Tính tổng
        } else {
            return 0; // Chưa có giỏ hàng thì số lượng là 0
        }
    }
    
    @Override
    public BigDecimal getSubtotal(Customer customer) {
        // 1. Tìm giỏ hàng của khách
    	Cart cart = cartRepository.findByCustomer(customer).orElse(null);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
     // 2. Tính tổng tiền (bao gồm cả variant và topping)
        return cart.getItems().stream()
                   .map(item -> {
                       // Bắt đầu với giá của variant (size)
                       BigDecimal itemPrice = item.getVariant().getPrice();
                       
                       // Cộng thêm giá của tất cả topping đã chọn
                       for (com.alotra.entity.product.Topping topping : item.getSelectedToppings()) {
                           itemPrice = itemPrice.add(topping.getAdditionalPrice());
                       }
                       
                       // Nhân với số lượng
                       return itemPrice.multiply(new BigDecimal(item.getQuantity()));
                   })
                   .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional // Quan trọng: Đảm bảo xóa an toàn
    public void clearCart(Customer customer) {
    	Cart cart = cartRepository.findByCustomer(customer).orElse(null);
        if (cart != null && cart.getItems() != null) {
            // Xóa tất cả CartItem liên kết với giỏ hàng này
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }
}