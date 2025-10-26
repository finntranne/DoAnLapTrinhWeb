//package com.alotra.service.cart.impl;
//
//import com.alotra.entity.cart.Cart;
//import com.alotra.entity.cart.CartItem;
//import com.alotra.entity.product.ProductVariant;
//import com.alotra.entity.product.Topping; // Import
//import com.alotra.entity.user.Customer;
//import com.alotra.repository.cart.CartItemRepository;
//import com.alotra.repository.cart.CartRepository;
//import com.alotra.repository.product.ProductVariantRepository;
//import com.alotra.repository.product.ToppingRepository; // Import
//import com.alotra.service.cart.CartService;
//import jakarta.persistence.EntityNotFoundException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional; // Import Transactional
//
//import java.util.List;    // Import
//import java.util.Objects;
//import java.util.Set;     // Import
//import java.util.HashSet; // Import
//import java.util.Optional;// Import
//import java.math.BigDecimal;
//import java.util.Collections;
//
//@Service
//public class CartServiceImpl implements CartService {
//
//    @Autowired private CartRepository cartRepository;
//    @Autowired private CartItemRepository cartItemRepository;
//    @Autowired private ProductVariantRepository variantRepository;
//    @Autowired private ToppingRepository toppingRepository; // Tiêm Topping Repo
//
//    @Override
//    @Transactional // Đảm bảo tất cả thao tác thành công hoặc không gì cả
//    public void addItemToCart(Customer customer, Integer variantId, int quantity, List<Integer> toppingIds) {
//        // 1. Kiểm tra đầu vào
//        if (customer == null) throw new IllegalArgumentException("Khách hàng không hợp lệ.");
//        if (quantity <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
//
//        // 2. Tìm biến thể sản phẩm (size + giá gốc)
//        ProductVariant variant = variantRepository.findById(variantId)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Size sản phẩm với ID: " + variantId));
//
//        // 3. Tìm hoặc tạo giỏ hàng cho khách
//        Cart cart = cartRepository.findByCustomer(customer)
//                .orElseGet(() -> {
//                    Cart newCart = new Cart();
//                    newCart.setCustomer(customer);
//                    return cartRepository.save(newCart); // Lưu giỏ hàng mới trước
//                });
//
//     // 4. Lấy danh sách Topping đã chọn (nếu có) - Khai báo là final
//        final Set<Topping> finalSelectedToppings;
//        if (toppingIds != null && !toppingIds.isEmpty()) {
//            finalSelectedToppings = new HashSet<>(toppingRepository.findAllById(toppingIds)); // Gán giá trị
//            // Kiểm tra xem có tìm thấy đủ topping không
//            if (finalSelectedToppings.size() != toppingIds.size()) {
//                System.err.println("Cảnh báo: Một vài ID topping không hợp lệ: " + toppingIds);
//                // Có thể bạn muốn ném ra lỗi ở đây nếu topping không hợp lệ là nghiêm trọng
//                // throw new EntityNotFoundException("Một vài topping không hợp lệ.");
//            }
//        } else {
//            finalSelectedToppings = Collections.emptySet(); // Gán giá trị là một Set rỗng
//        }
//        // =======================
//
//     // === 5. KIỂM TRA MÓN HÀNG TỒN TẠI (Đảm bảo so sánh Set chính xác) ===
//        Optional<CartItem> existingItemOpt = cart.getItems().stream()
//                .filter(item ->
//                    // Điều kiện 1: Cùng Variant (Size)
//                    item.getVariant().equals(variant) &&
//                    // Điều kiện 2: Cùng Set Topping (Set.equals() hoạt động tốt khi các phần tử có equals/hashCode đúng)
//                    Objects.equals(item.getSelectedToppings(), finalSelectedToppings)
//                    // Hoặc bạn có thể tự viết hàm so sánh Set nếu cần độ chính xác cao hơn
//                    // compareToppingSets(item.getSelectedToppings(), finalSelectedToppings)
//                )
//                .findFirst();
//
//        if (existingItemOpt.isPresent()) {
//            // 6a. Nếu có rồi -> Tăng số lượng
//            CartItem existingItem = existingItemOpt.get();
//            existingItem.setQuantity(existingItem.getQuantity() + quantity);
//            cartItemRepository.save(existingItem);
//        } else {
//            // 6b. Nếu chưa có -> Tạo CartItem mới
//            CartItem newItem = new CartItem();
//            newItem.setVariant(variant);
//            newItem.setQuantity(quantity);
//            newItem.setSelectedToppings(finalSelectedToppings); // <-- Dùng biến final
//            cart.addItem(newItem);
//            cartItemRepository.save(newItem);
//        }
//    }
//    
//    @Override
//    public BigDecimal calculateSubtotal(Set<CartItem> items) {
//        BigDecimal total = BigDecimal.ZERO;
//        if (items == null) {
//            return total;
//        }
//        for (CartItem item : items) {
//            BigDecimal itemPrice = item.getVariant().getPrice();
//            for (com.alotra.entity.product.Topping topping : item.getSelectedToppings()) {
//                itemPrice = itemPrice.add(topping.getAdditionalPrice());
//            }
//            total = total.add(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
//        }
//        return total;
//    }
//    
//    @Override
//    @Transactional
//    public void removeItemFromCart(Customer customer, Long cartItemId) {
//        if (customer == null) {
//            throw new IllegalArgumentException("Khách hàng không hợp lệ.");
//        }
//        if (cartItemId == null) {
//            throw new IllegalArgumentException("ID món hàng không hợp lệ.");
//        }
//
//        // 1. Tìm giỏ hàng của khách
//        Cart cart = cartRepository.findByCustomer(customer)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng cho khách hàng."));
//
//        // 2. Tìm món hàng cần xóa trong database
//        CartItem itemToRemove = cartItemRepository.findById(cartItemId)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));
//
//        // 3. **Kiểm tra bảo mật:** Đảm bảo món hàng này thuộc về đúng giỏ hàng của khách hiện tại
//        if (!itemToRemove.getCart().equals(cart)) {
//            // Ném lỗi nếu người dùng cố xóa món hàng không thuộc giỏ của họ
//            throw new AccessDeniedException("Bạn không có quyền xóa món hàng này.");
//        }
//
//        // 4. Xóa món hàng
//        // Cách 1: Dùng trực tiếp repository (phổ biến)
//        cartItemRepository.delete(itemToRemove);
//    }
//    
//    @Override
//    @Transactional
//    public CartItem updateItemQuantity(Customer customer, Long cartItemId, int newQuantity) {
//        if (customer == null) {
//            throw new IllegalArgumentException("Khách hàng không hợp lệ.");
//        }
//        if (cartItemId == null) {
//            throw new IllegalArgumentException("ID món hàng không hợp lệ.");
//        }
//        if (newQuantity <= 0) {
//            // Hoặc bạn có thể tự động xóa nếu số lượng <= 0
//            // removeItemFromCart(customer, cartItemId);
//            // return null; // Hoặc ném lỗi tùy logic mong muốn
//             throw new IllegalArgumentException("Số lượng mới phải lớn hơn 0.");
//        }
//        // Có thể thêm kiểm tra số lượng tối đa nếu muốn
//
//        // 1. Tìm giỏ hàng của khách
//        Cart cart = cartRepository.findByCustomer(customer)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng cho khách hàng."));
//
//        // 2. Tìm món hàng cần cập nhật
//        CartItem itemToUpdate = cartItemRepository.findById(cartItemId)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));
//
//        // 3. Kiểm tra bảo mật: Đảm bảo món hàng thuộc về giỏ của khách hiện tại
//        if (!itemToUpdate.getCart().equals(cart)) {
//            throw new AccessDeniedException("Bạn không có quyền cập nhật món hàng này.");
//        }
//
//        // 4. Cập nhật số lượng
//        itemToUpdate.setQuantity(newQuantity);
//
//        // 5. Lưu lại thay đổi và trả về item đã cập nhật
//        CartItem updatedItem = cartItemRepository.save(itemToUpdate);
//        System.out.println("Updated CartItem ID: " + cartItemId + " to quantity: " + newQuantity); // Log
//        return updatedItem;
//    }
//    
//    @Override
//    public int getCartItemCount(Customer customer) {
//        if (customer == null) {
//            return 0; // Chưa đăng nhập hoặc không có customer thì số lượng là 0
//        }
//        // Tìm giỏ hàng của khách
//        Optional<Cart> cartOpt = cartRepository.findByCustomer(customer);
//
//        if (cartOpt.isPresent()) {
//            Cart cart = cartOpt.get();
//            // Tính tổng quantity của tất cả CartItem trong giỏ
//            return cart.getItems().stream()
//                       .mapToInt(CartItem::getQuantity) // Lấy quantity của mỗi item
//                       .sum(); // Tính tổng
//        } else {
//            return 0; // Chưa có giỏ hàng thì số lượng là 0
//        }
//    }
//    
//    @Override
//    public BigDecimal getSubtotal(Customer customer) {
//        // 1. Tìm giỏ hàng của khách
//    	Cart cart = cartRepository.findByCustomer(customer).orElse(null);
//        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
//            return BigDecimal.ZERO;
//        }
//        
//     // 2. Tính tổng tiền (bao gồm cả variant và topping)
//        return cart.getItems().stream()
//                   .map(item -> {
//                       // Bắt đầu với giá của variant (size)
//                       BigDecimal itemPrice = item.getVariant().getPrice();
//                       
//                       // Cộng thêm giá của tất cả topping đã chọn
//                       for (com.alotra.entity.product.Topping topping : item.getSelectedToppings()) {
//                           itemPrice = itemPrice.add(topping.getAdditionalPrice());
//                       }
//                       
//                       // Nhân với số lượng
//                       return itemPrice.multiply(new BigDecimal(item.getQuantity()));
//                   })
//                   .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }
//
//    @Override
//    @Transactional // Quan trọng: Đảm bảo xóa an toàn
//    public void clearCart(Customer customer) {
//    	Cart cart = cartRepository.findByCustomer(customer).orElse(null);
//        if (cart != null && cart.getItems() != null) {
//            // Xóa tất cả CartItem liên kết với giỏ hàng này
//            cartItemRepository.deleteAll(cart.getItems());
//            cart.getItems().clear();
//            cartRepository.save(cart);
//        }
//    }
//}


package com.alotra.service.cart.impl; // Giữ package này

import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.product.ProductVariant;
import com.alotra.entity.product.Topping;
// *** SỬA: Dùng User thay vì Customer ***
import com.alotra.entity.user.User;
import com.alotra.repository.cart.CartItemRepository;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.product.ProductVariantRepository;
import com.alotra.repository.product.ToppingRepository;
// *** SỬA: Implement CartService (đảm bảo interface dùng User) ***
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
	
    @Override
    @Transactional
    // *** SỬA: Tham số là User ***
    public void addItemToCart(User user, Integer variantId, int quantity, List<Integer> toppingIds) {
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
                    // Các trường createdAt/updatedAt sẽ được @PrePersist xử lý
                    return cartRepository.save(newCart);
                });

        // 4. Lấy danh sách Topping đã chọn (dùng Integer ID)
        final Set<Topping> finalSelectedToppings;
        if (toppingIds != null && !toppingIds.isEmpty()) {
            finalSelectedToppings = new HashSet<>(toppingRepository.findAllById(toppingIds));
            if (finalSelectedToppings.size() != toppingIds.size()) {
                System.err.println("Cảnh báo: Một vài ID topping không hợp lệ: " + toppingIds);
                // Có thể ném lỗi nếu cần
            }
        } else {
            finalSelectedToppings = Collections.emptySet();
        }

        // 5. KIỂM TRA MÓN HÀNG TỒN TẠI (dùng Cart ID và Variant ID)
        // Lấy danh sách items trực tiếp từ repository để đảm bảo fetch LAZY hoạt động
        List<CartItem> currentItems = cartItemRepository.findByCart_CartID(cart.getCartID());

        Optional<CartItem> existingItemOpt = currentItems.stream()
                .filter(item ->
                    item.getVariant().getVariantID().equals(variantId) && // So sánh ID
                    Objects.equals(item.getSelectedToppings().stream().map(Topping::getToppingID).collect(Collectors.toSet()),
                                   finalSelectedToppings.stream().map(Topping::getToppingID).collect(Collectors.toSet())) // So sánh Set ID của Topping
                )
                .findFirst();

        if (existingItemOpt.isPresent()) {
            // 6a. Nếu có rồi -> Tăng số lượng
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            cartItemRepository.save(existingItem); // Lưu item đã cập nhật
        } else {
            // 6b. Nếu chưa có -> Tạo CartItem mới
            CartItem newItem = new CartItem();
            newItem.setVariant(variant);
            newItem.setQuantity(quantity);
            newItem.setSelectedToppings(finalSelectedToppings);
            newItem.setCart(cart); // Quan trọng: set Cart cho CartItem
            // addedAt sẽ được @PrePersist xử lý
            cartItemRepository.save(newItem); // Lưu item mới
            // Không cần gọi cart.addItem() nếu dùng cascade persist (nhưng gọi save trực tiếp rõ ràng hơn)
        }
        // Cập nhật timestamp của Cart (có thể không cần nếu Cart chỉ cập nhật khi User thay đổi)
        // cart.setUpdatedAt(LocalDateTime.now());
        // cartRepository.save(cart);
    }

    // *** Bỏ phương thức calculateSubtotal(Set<CartItem>) vì getSubtotal(User) hợp lý hơn ***

    @Override
    @Transactional
    // *** SỬA: Tham số là User và Integer cartItemId ***
    public void removeItemFromCart(User user, Integer cartItemId) {
        if (user == null) throw new IllegalArgumentException("Người dùng không hợp lệ.");
        if (cartItemId == null) throw new IllegalArgumentException("ID món hàng không hợp lệ.");

        // 1. Tìm giỏ hàng của User (dùng User ID)
        Cart cart = cartRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng cho người dùng."));

        // 2. Tìm món hàng cần xóa (dùng Integer ID)
        CartItem itemToRemove = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));

        // 3. Kiểm tra bảo mật: Đảm bảo món hàng thuộc về giỏ hàng của User
        // *** SỬA: So sánh Cart ID ***
        if (!itemToRemove.getCart().getCartID().equals(cart.getCartID())) {
            throw new AccessDeniedException("Bạn không có quyền xóa món hàng này.");
        }

        // 4. Xóa món hàng
        cartItemRepository.delete(itemToRemove);

        // Cập nhật timestamp của Cart
        // cart.setUpdatedAt(LocalDateTime.now());
        // cartRepository.save(cart);
    }

    @Override
    @Transactional
    // *** SỬA: Tham số là User và Integer cartItemId ***
    public CartItem updateItemQuantity(User user, Integer cartItemId, int newQuantity) {
        if (user == null) throw new IllegalArgumentException("Người dùng không hợp lệ.");
        if (cartItemId == null) throw new IllegalArgumentException("ID món hàng không hợp lệ.");
        if (newQuantity <= 0) {
            // Xem xét xóa item nếu số lượng <= 0
            removeItemFromCart(user, cartItemId); // Gọi hàm xóa đã có
            return null; // Trả về null sau khi xóa
            // Hoặc ném lỗi: throw new IllegalArgumentException("Số lượng mới phải lớn hơn 0.");
        }

        // 1. Tìm giỏ hàng (không cần thiết nếu đã có item)

        // 2. Tìm món hàng cần cập nhật (dùng Integer ID)
        CartItem itemToUpdate = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));

        // 3. Kiểm tra bảo mật: Đảm bảo món hàng thuộc về giỏ của User hiện tại
        // *** SỬA: Kiểm tra User ID trên Cart ***
        if (!itemToUpdate.getCart().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật món hàng này.");
        }

        // 4. Cập nhật số lượng
        itemToUpdate.setQuantity(newQuantity);

        // 5. Lưu lại thay đổi và trả về item đã cập nhật
        CartItem updatedItem = cartItemRepository.save(itemToUpdate);
        System.out.println("Updated CartItem ID: " + cartItemId + " to quantity: " + newQuantity); // Log

        // Cập nhật timestamp của Cart
        // Cart cart = itemToUpdate.getCart();
        // cart.setUpdatedAt(LocalDateTime.now());
        // cartRepository.save(cart);

        return updatedItem;
    }

    @Override
    // *** SỬA: Tham số là User ***
    public int getCartItemCount(User user) {
        if (user == null) return 0;

        // Tìm giỏ hàng của User
        Optional<Cart> cartOpt = cartRepository.findByUser_Id(user.getId()); // *** SỬA: findByUser_Id ***

        if (cartOpt.isPresent()) {
            // Lấy danh sách item từ repo để đảm bảo tính đúng
            List<CartItem> items = cartItemRepository.findByCart_CartID(cartOpt.get().getCartID());
            return items.stream()
                       .mapToInt(CartItem::getQuantity)
                       .sum();
        } else {
            return 0;
        }
    }

    @Override
    // *** SỬA: Tham số là User ***
    public BigDecimal getSubtotal(User user) {
        if (user == null) return BigDecimal.ZERO;

        // 1. Tìm giỏ hàng của User
        Optional<Cart> cartOpt = cartRepository.findByUser_Id(user.getId()); // *** SỬA: findByUser_Id ***
        if (cartOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 2. Lấy danh sách item chi tiết từ repo (tối ưu hơn)
        List<CartItem> items = cartItemRepository.findByCartIdWithDetails(cartOpt.get().getCartID()); // Dùng query fetch join
        if (items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 3. Tính tổng tiền (bao gồm cả variant và topping)
        return items.stream()
                   .map(item -> {
                       BigDecimal itemPrice = item.getVariant().getPrice(); // Giá variant
                       // Cộng giá topping (Topping đã được fetch EAGER hoặc JOIN FETCH)
                       for (Topping topping : item.getSelectedToppings()) {
                           itemPrice = itemPrice.add(topping.getAdditionalPrice());
                       }
                       return itemPrice.multiply(new BigDecimal(item.getQuantity())); // Nhân số lượng
                   })
                   .reduce(BigDecimal.ZERO, BigDecimal::add); // Cộng tổng
    }

    @Override
    @Transactional
    // *** SỬA: Tham số là User ***
    public void clearCart(User user) {
        if (user == null) return;

        Optional<Cart> cartOpt = cartRepository.findByUser_Id(user.getId()); // *** SỬA: findByUser_Id ***
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            // Xóa tất cả CartItem liên kết với CartID này (hiệu quả hơn)
            cartItemRepository.deleteByCart_CartID(cart.getCartID()); // *** SỬA: dùng deleteByCart_CartID ***
            // Không cần clear list items trên entity Cart nữa
            // cart.getItems().clear();
            // cartRepository.save(cart); // Không cần save lại Cart sau khi xóa items
            System.out.println("Cleared cart for User ID: " + user.getId());
        }
    }

    @Override
    public BigDecimal calculateSubtotal(Set<CartItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        if (items == null || items.isEmpty()) {
            return total;
        }
        for (CartItem item : items) {
            // Ensure variant is loaded if necessary (though JOIN FETCH in repo query is better)
            if (item.getVariant() == null) {
                // Log a warning or fetch the variant if needed, depending on context
                // This shouldn't happen if items come from findByCartIdWithDetails
                log.warn("CartItem ID " + item.getCartItemID() + " has null variant in calculateSubtotal.");
                continue; // Skip this item
            }
            BigDecimal itemPrice = item.getVariant().getPrice(); // Price of the variant (size)

            // Add price of selected toppings (ensure toppings are loaded - EAGER or JOIN FETCH recommended)
            if (item.getSelectedToppings() != null) {
                for (Topping topping : item.getSelectedToppings()) {
                    if (topping != null && topping.getAdditionalPrice() != null) {
                        itemPrice = itemPrice.add(topping.getAdditionalPrice());
                    } else {
                        // Log warning about potentially null topping data
                        log.warn("Null topping or topping price found for CartItem ID " + item.getCartItemID());
                    }
                }
            }

            // Multiply by quantity and add to total
            total = total.add(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }
}