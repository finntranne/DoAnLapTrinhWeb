package com.alotra.controller; // Giữ package này

// Import các entity
import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.user.User;

// Import Service và Repository
import com.alotra.repository.cart.CartRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.shop.StoreService;
import com.alotra.service.user.UserService;
import com.alotra.repository.cart.CartItemRepository; // *** THÊM IMPORT NÀY ***

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
// *** THÊM CÁC IMPORT ĐỂ TẠO VIEWMODEL ***
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


@Controller
public class CartController {

    @Autowired private CartService cartService;
    @Autowired private UserService userService; 
    @Autowired private CartRepository cartRepository; 
    @Autowired private CategoryService categoryService;
    @Autowired private StoreService storeService;

    // *** THÊM: Inject CartItemRepository để lấy item chi tiết ***
    @Autowired
    private CartItemRepository cartItemRepository;

    // --- Hàm trợ giúp lấy User đang đăng nhập ---
    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        String username = auth.getName(); 
        return userService.findByUsername(username) 
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + username));
    }

    // --- Hàm trợ giúp lấy số lượng giỏ hàng (ĐÃ SỬA) ---
    private int getCurrentCartItemCount(User user) {
        if (user == null) return 0;
        return cartService.getCartItemCount(user);
    }
    
    private Integer getSelectedShopId(HttpSession session) {
        Integer selectedShopId = (Integer) session.getAttribute("selectedShopId");
        return (selectedShopId == null) ? 0 : selectedShopId; // Mặc định là 0 (Xem tất cả)
    }

    // --- Thêm vào giỏ (Giữ nguyên) ---
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") Integer productId, 
                            @RequestParam("variantId") Integer variantId,
                            @RequestParam("quantity") Integer quantity,
                            @RequestParam(name = "toppingIds", required = false) List<Integer> toppingIds,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentAuthenticatedUser(); 
            cartService.addItemToCart(currentUser, variantId, quantity, toppingIds); 
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Lỗi thêm vào giỏ hàng: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/products/" + productId;
    }

    // --- SỬA HÀM NÀY: Xem giỏ hàng (ĐÃ SỬA ĐỂ TÍNH GIÁ ĐÚNG) ---
    @GetMapping("/cart")
    public String viewCart(Model model, HttpSession session) {
        User currentUser;
        try {
            currentUser = getCurrentAuthenticatedUser(); // Lấy User
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login"; // Chưa đăng nhập thì về login
        }
        Integer selectedShopId = getSelectedShopId(session);
        
        Optional<Cart> cartOpt = cartRepository.findByUser_Id(currentUser.getId());

        // *** THAY ĐỔI: Tạo ViewModel để gửi giá đúng sang view ***
        List<Map<String, Object>> cartItemViewModels = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO; // Khởi tạo

        if (cartOpt.isPresent()) {
            // Lấy item CÓ ĐẦY ĐỦ CHI TIẾT (giống hệt service)
            // Dùng Set để đảm bảo không trùng lặp nếu query trả về nhiều
            Set<CartItem> cartItems = new HashSet<>(
                cartItemRepository.findByCartIdWithDetails(cartOpt.get().getCartID())
            );
            
            for (CartItem item : cartItems) {
                Map<String, Object> vm = new HashMap<>();
                vm.put("item", item); // Đối tượng CartItem
                vm.put("lineTotal", cartService.getLineTotal(item)); // Giá ĐÚNG (đã giảm giá)
                cartItemViewModels.add(vm);
            }
            
            // Service đã được sửa nên hàm này trả về tổng tiền ĐÚNG
            subtotal = cartService.getSubtotal(currentUser); 
        }

        model.addAttribute("cartItemVMs", cartItemViewModels); // <-- Gửi ViewModel sang view
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("cartItemCount", getCurrentCartItemCount(currentUser)); 
        model.addAttribute("categories", categoryService.findAll()); 
        model.addAttribute("shops", storeService.findAllActiveShops());
        model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
        return "cart/cart"; // View cart.html
    }

    // --- Xóa khỏi giỏ (Giữ nguyên) ---
    @PostMapping("/cart/remove/{itemId}")
    public String removeFromCart(@PathVariable("itemId") Integer itemId, 
                                 RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentAuthenticatedUser(); 
            cartService.removeItemFromCart(currentUser, itemId); 
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException enfe) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + enfe.getMessage());
        } catch (Exception e) {
            System.err.println("Lỗi xóa khỏi giỏ hàng: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi không mong muốn khi xóa.");
        }
        return "redirect:/cart"; 
    }

    // --- SỬA HÀM NÀY: Cập nhật số lượng (ĐÃ SỬA ĐỂ TÍNH GIÁ ĐÚNG) ---
    @PostMapping("/cart/update/{itemId}")
    public ResponseEntity<?> updateCartItemQuantity(@PathVariable("itemId") Integer itemId, // Dùng Integer ID
                                                    @RequestParam("quantity") Integer newQuantity) {
        User currentUser;
        try {
            currentUser = getCurrentAuthenticatedUser(); // Lấy User
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        if (newQuantity == null) {
             return ResponseEntity.badRequest().body(Map.of("error", "Số lượng không hợp lệ"));
        }

        try {
            // Gọi service để cập nhật
            CartItem updatedItem = cartService.updateItemQuantity(currentUser, itemId, newQuantity);

            // Nếu newQuantity <= 0, item đã bị xóa
            if (updatedItem == null) {
                 BigDecimal newSubtotal = cartService.getSubtotal(currentUser); 
                 int newCartCount = cartService.getCartItemCount(currentUser);
                 return ResponseEntity.ok(Map.of(
                     "removed", true, 
                     "newSubtotal", newSubtotal,
                     "newCartCount", newCartCount 
                 ));
            }

            // *** THAY ĐỔI: Gọi service để lấy giá đúng, bỏ tính toán thủ công ***
            BigDecimal lineTotal = cartService.getLineTotal(updatedItem); // <-- GIÁ ĐÚNG
            
            BigDecimal newSubtotal = cartService.getSubtotal(currentUser); // Tính lại tổng giỏ hàng
            int newCartCount = cartService.getCartItemCount(currentUser);

            // Trả về dữ liệu JSON cho JavaScript
            return ResponseEntity.ok(Map.of(
                    "removed", false, 
                    "newLineTotal", lineTotal, // <-- Trả về giá đúng
                    "newSubtotal", newSubtotal,
                    "newQuantity", updatedItem.getQuantity(),
                    "newCartCount", newCartCount 
            ));

        } catch (EntityNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Lỗi cập nhật số lượng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi máy chủ nội bộ."));
        }
    }

 // --- Mua ngay (Giữ nguyên) ---
    @PostMapping("/cart/buy-now")
    public String buyNow(@RequestParam("productId") Integer productId,
                         @RequestParam("variantId") Integer variantId,
                         @RequestParam("quantity") Integer quantity,
                         @RequestParam(name = "toppingIds", required = false) List<Integer> toppingIds,
                         RedirectAttributes redirectAttributes,
                         HttpSession session) { 

        try {
            User currentUser = getCurrentAuthenticatedUser(); 

            // 1. Thêm item vào giỏ VÀ LẤY VỀ ITEM ĐÓ
            CartItem addedItem = cartService.addItemToCart(currentUser, variantId, quantity, toppingIds);

            if (addedItem == null || addedItem.getCartItemID() == null) {
                throw new IllegalStateException("Không thể thêm sản phẩm để mua ngay.");
            }

            // 2. LẤY ID CỦA ITEM VỪA THÊM
            Integer newItemId = addedItem.getCartItemID(); // ID là Integer

            // 3. TẠO DANH SÁCH CHỈ CHỨA ID NÀY
            List<Integer> selectedIdsForCheckout = Collections.singletonList(newItemId);

            // 4. LƯU DANH SÁCH NÀY VÀO SESSION
            session.setAttribute("selectedCheckoutItemIds", selectedIdsForCheckout);

            // 5. Chuyển hướng đến trang checkout
            return "redirect:/checkout"; 

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Lỗi Mua Ngay (thêm vào giỏ): " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi chuẩn bị mua ngay: " + e.getMessage());
            return "redirect:/products/" + productId; 
        }
    }
}