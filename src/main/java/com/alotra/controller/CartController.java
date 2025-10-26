//package com.alotra.controller;
//
//import com.alotra.entity.cart.Cart;
//import com.alotra.entity.cart.CartItem;
//import com.alotra.entity.user.Customer;
//import com.alotra.entity.user.User;
//import com.alotra.repository.cart.CartRepository;
//import com.alotra.service.cart.CartService;
//import com.alotra.service.product.CategoryService;
//import com.alotra.service.user.CustomerService;
//import com.alotra.service.user.UserService;    // Cần có Service này
//
//import jakarta.persistence.EntityNotFoundException;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.server.ResponseStatusException;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.math.BigDecimal;
//import java.util.Collections;
//import java.util.List; // Import
//import java.util.Map;
//import java.util.Optional;
//import java.util.Set;
//
//@Controller
//public class CartController {
//
//    @Autowired private CartService cartService;
//    @Autowired
//    @Qualifier("userServiceImpl") // <-- Thêm dòng này
//    private UserService userService;
//    @Autowired private CustomerService customerService;
//    @Autowired private CartRepository cartRepository;
//    @Autowired
//    private CategoryService categoryService;
//    
// // --- Hàm trợ giúp lấy số lượng giỏ hàng ---
//    private int getCurrentCartItemCount() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
//            String username = auth.getName();
//            Optional<User> userOpt = userService.findByUsername(username); // Hoặc findByEmail
//            if (userOpt.isPresent()) {
//                Optional<Customer> customerOpt = customerService.findByUser(userOpt.get());
//                if (customerOpt.isPresent()) {
//                    return cartService.getCartItemCount(customerOpt.get());
//                }
//            }
//        }
//        return 0; // Trả về 0 nếu chưa đăng nhập hoặc có lỗi
//    }
//
//    // Xử lý khi nhấn nút "THÊM VÀO GIỎ"
//    @PostMapping("/cart/add")
//    public String addToCart(@RequestParam("productId") Integer productId,     // Giữ lại để redirect về đúng trang
//                            @RequestParam("variantId") Integer variantId,     // ID của Size đã chọn
//                            @RequestParam("quantity") Integer quantity,      // Số lượng
//                            @RequestParam(name = "toppingIds", required = false) List<Integer> toppingIds, // Danh sách ID topping đã chọn
//                            RedirectAttributes redirectAttributes) {
//
//        // --- Lấy thông tin khách hàng đang đăng nhập ---
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
//            return "redirect:/login"; // Bắt buộc đăng nhập
//        }
//        String username = auth.getName();
//        User currentUser = userService.findByUsername(username)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
//        Customer currentCustomer = customerService.findByUser(currentUser)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ khách hàng"));
//        // ---------------------------------------------
//
//        try {
//            // Gọi service để thêm vào giỏ
//            cartService.addItemToCart(currentCustomer, variantId, quantity, toppingIds);
//            // Gửi thông báo thành công về trang redirect
//            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
//        } catch (Exception e) {
//            // Gửi thông báo lỗi về trang redirect
//            System.err.println("Lỗi thêm vào giỏ hàng: " + e.getMessage()); // Log lỗi ra console
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
//        }
//
//        // Quay lại trang chi tiết sản phẩm vừa xem
//        return "redirect:/products/" + productId;
//    }
//
// // === THÊM PHƯƠNG THỨC XEM GIỎ HÀNG ===
//    @GetMapping("/cart")
//    public String viewCart(Model model) {
//        // --- Lấy thông tin khách hàng đang đăng nhập ---
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
//            return "redirect:/login"; // Bắt buộc đăng nhập
//        }
//        String username = auth.getName();
//        User currentUser = userService.findByUsername(username)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
//        Customer currentCustomer = customerService.findByUser(currentUser)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ khách hàng"));
//        // ---------------------------------------------
//
//        // Lấy giỏ hàng của khách hàng từ repository
//        Cart cart = cartRepository.findByCustomer(currentCustomer)
//                                 .orElse(null); // Trả về null nếu chưa có giỏ hàng
//
//        Set<CartItem> cartItems = (cart != null) ? cart.getItems() : Collections.emptySet();
//        BigDecimal subtotal = cartService.calculateSubtotal(cartItems);
//
//        model.addAttribute("cartItems", cartItems);
//        model.addAttribute("subtotal", subtotal);
//        model.addAttribute("cartItemCount", getCurrentCartItemCount());
//        model.addAttribute("categories", categoryService.findAll());
//        // Bạn có thể thêm các tính toán khác như giảm giá, phí ship, tổng cộng ở đây
//
//        return "cart/cart"; // Trả về file view cart.html trong thư mục templates/cart
//    }
//
//    // --- Hàm tiện ích tính tổng tiền (Subtotal) ---
//    private BigDecimal calculateSubtotal(Set<CartItem> items) {
//        BigDecimal total = BigDecimal.ZERO;
//        if (items == null) {
//            return total;
//        }
//        for (CartItem item : items) {
//            // Giá gốc của variant (size)
//            BigDecimal itemPrice = item.getVariant().getPrice();
//            // Cộng thêm giá của các topping đã chọn
//            for (com.alotra.entity.product.Topping topping : item.getSelectedToppings()) {
//                itemPrice = itemPrice.add(topping.getAdditionalPrice());
//            }
//            // Nhân với số lượng
//            total = total.add(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
//        }
//        return total;
//    }
//    
//    @PostMapping("/cart/remove/{itemId}")
//    public String removeFromCart(@PathVariable("itemId") Long itemId,
//                                 RedirectAttributes redirectAttributes) {
//
//        // --- Lấy thông tin khách hàng đang đăng nhập ---
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
//            return "redirect:/login";
//        }
//        String username = auth.getName(); // Hoặc email tùy cấu hình
//        User currentUser = userService.findByUsername(username) // Hoặc findByEmail
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
//        Customer currentCustomer = customerService.findByUser(currentUser)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ khách hàng"));
//        // ---------------------------------------------
//
//        try {
//            cartService.removeItemFromCart(currentCustomer, itemId);
//            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
//        } catch (EntityNotFoundException enfe) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + enfe.getMessage());
//        } catch (AccessDeniedException ade) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi bảo mật: " + ade.getMessage());
//        } catch (Exception e) {
//            System.err.println("Lỗi xóa khỏi giỏ hàng: " + e.getMessage());
//            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi không mong muốn khi xóa.");
//        }
//
//        // Quay lại trang giỏ hàng
//        return "redirect:/cart";
//    }
//    
//    @PostMapping("/cart/update/{itemId}")
//    public ResponseEntity<?> updateCartItemQuantity(@PathVariable("itemId") Long itemId,
//                                                     @RequestParam("quantity") Integer newQuantity) {
//        // --- Lấy thông tin khách hàng đang đăng nhập ---
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
//            // Trả về lỗi 401 Unauthorized nếu chưa đăng nhập
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập"));
//        }
//        String username = auth.getName(); // Hoặc email
//        User currentUser = userService.findByUsername(username) // Hoặc findByEmail
//                .orElse(null); // Tìm user
//        if (currentUser == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Không tìm thấy người dùng"));
//        }
//        Customer currentCustomer = customerService.findByUser(currentUser)
//                .orElse(null); // Tìm customer
//        if (currentCustomer == null) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Không tìm thấy hồ sơ khách hàng"));
//        }
//        // ---------------------------------------------
//
//        if (newQuantity == null || newQuantity <= 0) {
//             return ResponseEntity.badRequest().body(Map.of("error", "Số lượng không hợp lệ"));
//        }
//
//        try {
//            // Gọi service để cập nhật
//            CartItem updatedItem = cartService.updateItemQuantity(currentCustomer, itemId, newQuantity);
//
//            // Tính toán lại giá trị mới (tạm tính item và tổng giỏ hàng) để trả về cho JS
//            BigDecimal itemPrice = updatedItem.getVariant().getPrice();
//            for (com.alotra.entity.product.Topping topping : updatedItem.getSelectedToppings()) {
//                itemPrice = itemPrice.add(topping.getAdditionalPrice());
//            }
//            BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(updatedItem.getQuantity()));
//
//            // Lấy lại giỏ hàng để tính tổng mới
//            Cart cart = cartRepository.findByCustomer(currentCustomer).orElse(null);
//            BigDecimal newSubtotal = cart != null ? cartService.calculateSubtotal(cart.getItems()) : BigDecimal.ZERO;
//
//            // Trả về dữ liệu JSON cho JavaScript
//            return ResponseEntity.ok(Map.of(
//                    "newLineTotal", lineTotal,
//                    "newSubtotal", newSubtotal,
//                    "newQuantity", updatedItem.getQuantity() // Trả về số lượng thực tế đã lưu
//            ));
//
//        } catch (EntityNotFoundException | AccessDeniedException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
//        } catch (IllegalArgumentException e) {
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        } catch (Exception e) {
//            System.err.println("Lỗi cập nhật số lượng: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi máy chủ nội bộ."));
//        }
//    }
//    
//    @PostMapping("/cart/buy-now")
//    public String buyNow(@RequestParam("productId") Integer productId, // Vẫn cần để xử lý lỗi
//                         @RequestParam("variantId") Integer variantId,
//                         @RequestParam("quantity") Integer quantity,
//                         @RequestParam(name = "toppingIds", required = false) List<Integer> toppingIds,
//                         RedirectAttributes redirectAttributes) {
//
//        // --- Lấy thông tin khách hàng đang đăng nhập (Giống hệt addToCart) ---
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
//            return "redirect:/login"; // Bắt buộc đăng nhập
//        }
//        String username = auth.getName();
//        User currentUser = userService.findByUsername(username)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
//        Customer currentCustomer = customerService.findByUser(currentUser)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ khách hàng"));
//        // ---------------------------------------------
//
//        try {
//            // 1. Gọi service để thêm item vào giỏ (Logic giống hệt addToCart)
//            cartService.addItemToCart(currentCustomer, variantId, quantity, toppingIds);
//
//            // 2. Chuyển hướng NGAY LẬP TỨC đến trang checkout
//            return "redirect:/checkout"; // <-- Khác biệt chính là ở đây
//
//        } catch (Exception e) {
//            // Nếu có lỗi khi thêm vào giỏ, quay lại trang sản phẩm và báo lỗi
//            System.err.println("Lỗi Mua Ngay (thêm vào giỏ): " + e.getMessage());
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi chuẩn bị mua ngay: " + e.getMessage());
//            return "redirect:/products/" + productId; // <-- Quay lại trang sản phẩm nếu lỗi
//        }
//    }
//}

package com.alotra.controller; // Giữ package này

// Import các entity đã merge
import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.user.User; // Sử dụng User

// Import Service và Repository đã merge (hoặc chuẩn)
import com.alotra.repository.cart.CartRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.user.UserService; // Sử dụng UserService thống nhất

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
// Bỏ import Qualifier nếu không cần thiết
// import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Thêm exception này
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
import java.util.Set; // Vẫn dùng Set nếu Cart entity trả về Set

@Controller
public class CartController {

    @Autowired private CartService cartService;
    // @Autowired @Qualifier("userServiceImpl") // Qualifier có thể không cần nếu chỉ có 1 bean UserService
    @Autowired private UserService userService; // Sử dụng UserService
    @Autowired private CartRepository cartRepository; // Vẫn cần để lấy Cart entity đầy đủ cho view
    @Autowired private CategoryService categoryService; // Giữ lại nếu cần cho layout

    // --- Hàm trợ giúp lấy User đang đăng nhập ---
    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        String username = auth.getName(); // Thường là email
        return userService.findByUsername(username) // Giả sử UserService có findByUsername trả về Optional<User>
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + username));
    }

    // --- Hàm trợ giúp lấy số lượng giỏ hàng (ĐÃ SỬA) ---
    private int getCurrentCartItemCount(User user) {
        if (user == null) return 0;
        // Gọi thẳng CartService đã được sửa
        return cartService.getCartItemCount(user);
    }

    // --- Thêm vào giỏ (ĐÃ SỬA) ---
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") Integer productId, // Giữ lại để redirect
                            @RequestParam("variantId") Integer variantId,
                            @RequestParam("quantity") Integer quantity,
                            @RequestParam(name = "toppingIds", required = false) List<Integer> toppingIds,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentAuthenticatedUser(); // Lấy User
            cartService.addItemToCart(currentUser, variantId, quantity, toppingIds); // Gọi service với User
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            // Lỗi xác thực hoặc không tìm thấy user -> về trang login
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Lỗi thêm vào giỏ hàng: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        // Redirect về trang sản phẩm
        return "redirect:/products/" + productId;
    }

    // --- Xem giỏ hàng (ĐÃ SỬA) ---
    @GetMapping("/cart")
    public String viewCart(Model model) {
        User currentUser;
        try {
            currentUser = getCurrentAuthenticatedUser(); // Lấy User
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login"; // Chưa đăng nhập thì về login
        }

        // Lấy giỏ hàng của User từ repository (để lấy cả entity Cart và items nếu cần)
        Optional<Cart> cartOpt = cartRepository.findByUser_Id(currentUser.getId());

        Set<CartItem> cartItems = Collections.emptySet(); // Mặc định là rỗng
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            // Lấy items từ Cart entity (đảm bảo fetch type phù hợp hoặc dùng query fetch join nếu cần)
            cartItems = cart.getItems();
        }

        // Tính tổng tiền bằng CartService
        BigDecimal subtotal = cartService.getSubtotal(currentUser);

        model.addAttribute("cartItems", cartItems); // Truyền Set<CartItem>
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("cartItemCount", getCurrentCartItemCount(currentUser)); // Truyền user vào
        model.addAttribute("categories", categoryService.findAll()); // Giữ lại nếu layout cần

        return "cart/cart"; // View cart.html
    }

    // --- Xóa khỏi giỏ (ĐÃ SỬA) ---
    @PostMapping("/cart/remove/{itemId}")
    public String removeFromCart(@PathVariable("itemId") Integer itemId, // Dùng Integer ID
                                 RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentAuthenticatedUser(); // Lấy User
            cartService.removeItemFromCart(currentUser, itemId); // Gọi service với User và Integer ID
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException enfe) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + enfe.getMessage());
        } catch (Exception e) {
            System.err.println("Lỗi xóa khỏi giỏ hàng: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi không mong muốn khi xóa.");
        }
        return "redirect:/cart"; // Quay lại trang giỏ hàng
    }

    // --- Cập nhật số lượng (ĐÃ SỬA) ---
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

            // Nếu newQuantity <= 0, item đã bị xóa, trả về thông báo khác
            if (updatedItem == null) {
                 BigDecimal newSubtotal = cartService.getSubtotal(currentUser); // Tính lại subtotal sau khi xóa
                 int newCartCount = cartService.getCartItemCount(currentUser);
                 return ResponseEntity.ok(Map.of(
                     "removed", true, // Thêm flag báo item đã bị xóa
                     "newSubtotal", newSubtotal,
                     "newCartCount", newCartCount // Trả về số lượng tổng mới
                 ));
            }

            // Tính toán lại giá trị mới (tạm tính item và tổng giỏ hàng)
            BigDecimal itemPrice = updatedItem.getVariant().getPrice();
            // Cần fetch topping nếu không phải EAGER
            Set<com.alotra.entity.product.Topping> toppings = updatedItem.getSelectedToppings();
            if (toppings != null) {
                for (com.alotra.entity.product.Topping topping : toppings) {
                    itemPrice = itemPrice.add(topping.getAdditionalPrice());
                }
            }
            BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(updatedItem.getQuantity()));

            BigDecimal newSubtotal = cartService.getSubtotal(currentUser); // Tính lại tổng giỏ hàng
            int newCartCount = cartService.getCartItemCount(currentUser);

            // Trả về dữ liệu JSON cho JavaScript
            return ResponseEntity.ok(Map.of(
                    "removed", false, // Item không bị xóa
                    "newLineTotal", lineTotal,
                    "newSubtotal", newSubtotal,
                    "newQuantity", updatedItem.getQuantity(),
                    "newCartCount", newCartCount // Trả về số lượng tổng mới
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

    // --- Mua ngay (ĐÃ SỬA) ---
    @PostMapping("/cart/buy-now")
    public String buyNow(@RequestParam("productId") Integer productId, // Giữ lại để redirect lỗi
                         @RequestParam("variantId") Integer variantId,
                         @RequestParam("quantity") Integer quantity,
                         @RequestParam(name = "toppingIds", required = false) List<Integer> toppingIds,
                         RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentAuthenticatedUser(); // Lấy User
            // 1. Thêm item vào giỏ (Logic giống addToCart)
            cartService.addItemToCart(currentUser, variantId, quantity, toppingIds);
            // 2. Chuyển hướng đến trang checkout
            return "redirect:/checkout"; // Chuyển đến trang thanh toán
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
            System.err.println("Lỗi Mua Ngay (thêm vào giỏ): " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi chuẩn bị mua ngay: " + e.getMessage());
            return "redirect:/products/" + productId; // Quay lại trang sản phẩm nếu lỗi
        }
    }

    // Bỏ hàm calculateSubtotal(Set<CartItem>) riêng trong controller vì đã có trong service
}