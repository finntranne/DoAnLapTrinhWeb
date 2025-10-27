//package com.alotra.controller;
//
//import com.alotra.entity.user.Address;
//import com.alotra.entity.user.Customer;
//import com.alotra.entity.user.User;
//import com.alotra.service.cart.CartService;
//import com.alotra.service.product.CategoryService;
//import com.alotra.service.user.CustomerService;
//import com.alotra.service.user.UserService;
//
//import java.util.Optional;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.server.ResponseStatusException;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//@Controller
//@RequestMapping("/user/profile")
//public class UserProfileController {
//
//    @Autowired 
//    private CustomerService customerService;
//
//    @Autowired
//    @Qualifier("userServiceImpl")
//    private UserService userService;
//    
//    @Autowired private CartService cartService;
//    @Autowired
//    private CategoryService categoryService;
//
//    /**
//     * Lấy thông tin Customer hiện tại từ Security Context
//     */
//    private Customer getCurrentCustomer() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
//        }
//        String username = auth.getName();
//        User currentUser = userService.findByUsername(username)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
//        return customerService.findByUser(currentUser)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ khách hàng"));
//    }
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
//    /**
//     * Hiển thị trang hồ sơ cá nhân
//     * GET /user/profile
//     */
//    @GetMapping
//    public String showProfilePage(Model model) {
//        try {
//            Customer customer = getCurrentCustomer();
//            
//            // Lấy địa chỉ mặc định nếu có
//            Address defaultAddress = customer.getAddresses() != null 
//                ? customer.getAddresses().stream()
//                    .filter(Address::isDefault)
//                    .findFirst()
//                    .orElse(null)
//                : null;
//            
//            model.addAttribute("customer", customer);
//            model.addAttribute("defaultAddress", defaultAddress);
//            model.addAttribute("totalAddresses", customer.getAddresses() != null ? customer.getAddresses().size() : 0);
//            
//            model.addAttribute("cartItemCount", getCurrentCartItemCount());
//            
//            model.addAttribute("categories", categoryService.findAll());
//            
//            return "user/profile";
//            
//        } catch (ResponseStatusException e) {
//            return "redirect:/login";
//        }
//    }
//
//    /**
//     * Cập nhật thông tin cá nhân
//     * POST /user/profile/update
//     */
//    @PostMapping("/update")
//    public String updateProfile(@RequestParam("fullName") String newFullName,
//                                @RequestParam(value = "email", required = false) String newEmail,
//                                RedirectAttributes redirectAttributes) {
//        try {
//            Customer customer = getCurrentCustomer();
//            User user = customer.getUser();
//
//            // Validate fullName
//            if (newFullName == null || newFullName.trim().isEmpty()) {
//                redirectAttributes.addFlashAttribute("errorMessage", "Họ và tên không được để trống!");
//                return "redirect:/user/profile";
//            }
//
//            // Cập nhật thông tin User
//            user.setFullname(newFullName.trim());
//            
//            // Cập nhật thông tin Customer
//            customer.setFullName(newFullName.trim());
//            
//            // Cập nhật email nếu có thay đổi và không trùng
//            if (newEmail != null && !newEmail.trim().isEmpty() && !newEmail.equals(customer.getEmail())) {
//                customer.setEmail(newEmail.trim());
//            }
//
//            // Lưu thay đổi
//            userService.save(user);
//            customerService.save(customer);
//
//            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
//            return "redirect:/user/profile";
//
//        } catch (ResponseStatusException e) {
//            return "redirect:/login";
//        } catch (Exception e) {
//            e.printStackTrace();
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật hồ sơ: " + e.getMessage());
//            return "redirect:/user/profile";
//        }
//    }
//}

package com.alotra.controller; // Giữ package này

// Import entity đã merge
import com.alotra.entity.location.Address;
import com.alotra.entity.user.User; // Sử dụng User

// Import Service và Repository đã merge
import com.alotra.repository.location.AddressRepository; // Sử dụng AddressRepository
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
// import com.alotra.service.user.CustomerService; // *** BỎ CustomerService ***
import com.alotra.service.user.UserService; // Sử dụng UserService

import java.util.List; // Import List
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Qualifier; // Bỏ nếu không cần
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Thêm exception
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult; // Import BindingResult
import org.springframework.validation.BindingResult; // Import BindingResult
import org.springframework.web.bind.annotation.*; // Dùng *
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/profile") // Giữ tiền tố
public class UserProfileController { // Giữ tên class

    // @Autowired private CustomerService customerService; // *** BỎ CustomerService ***
    // @Autowired @Qualifier("userServiceImpl") // Bỏ nếu không cần
    @Autowired private UserService userService; // *** GIỮ UserService ***
    @Autowired private AddressRepository addressRepository; // *** THÊM AddressRepository ***
    @Autowired private CartService cartService;
    @Autowired private CategoryService categoryService;

    // === Hàm trợ giúp lấy User (Giống CartController) ===
    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        String username = auth.getName(); // Thường là email
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + username));
    }

    // --- Hàm trợ giúp lấy số lượng giỏ hàng (Giống CartController) ---
    private int getCurrentCartItemCount() {
        try {
            User user = getCurrentAuthenticatedUser();
            return cartService.getCartItemCount(user);
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return 0;
        }
    }

    /**
     * Hiển thị trang hồ sơ cá nhân (ĐÃ SỬA)
     * GET /user/profile
     */
    @GetMapping
    public String showProfilePage(Model model) {
        try {
            User user = getCurrentAuthenticatedUser(); 

            List<Address> addresses = addressRepository.findByUserId(user.getId());

            Address defaultAddress = addresses.stream()
                    .filter(address -> address.getIsDefault())
                    .findFirst()
                    .orElse(null);
            model.addAttribute("customer", user); 
            // ===================
            
            model.addAttribute("defaultAddress", defaultAddress);
            model.addAttribute("totalAddresses", addresses.size()); 
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());

            return "user/profile"; 

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        }
    }

    /**
     * Cập nhật thông tin cá nhân (ĐÃ SỬA)
     * POST /user/profile/update
     */
    @PostMapping("/update")
    public String updateProfile(@RequestParam("fullName") String newFullName,
                                @RequestParam(value = "phoneNumber", required = false) String newPhoneNumber, // Thêm SĐT nếu cần
                                // @RequestParam(value = "email", required = false) String newEmail, // Email thường không cho đổi dễ dàng
                                RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser(); // Lấy User hiện tại

            // Validate fullName
            if (newFullName == null || newFullName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Họ và tên không được để trống!");
                return "redirect:/user/profile";
            }

            // Cập nhật thông tin User
            boolean changed = false;
            if (!user.getFullName().equals(newFullName.trim())) {
                user.setFullName(newFullName.trim()); // Giả sử User có setFullname
                changed = true;
            }

            // Cập nhật SĐT nếu có và khác
            if (newPhoneNumber != null && !newPhoneNumber.trim().isEmpty() &&
                (user.getPhoneNumber() == null || !user.getPhoneNumber().equals(newPhoneNumber.trim()))) {
                 // *** CẦN KIỂM TRA SĐT TRÙNG LẶP NẾU SĐT LÀ UNIQUE ***
                 Optional<User> existingUserOpt = userService.findByPhoneNumber(newPhoneNumber.trim());
                 if (existingUserOpt.isPresent() && !existingUserOpt.get().getId().equals(user.getId())) {
                      redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại đã được sử dụng bởi tài khoản khác.");
                      return "redirect:/user/profile";
                 }
                user.setPhoneNumber(newPhoneNumber.trim()); // Giả sử User có setPhoneNumber
                changed = true;
            }

            /* // Cập nhật email (KHÔNG NÊN LÀM Ở ĐÂY - Cần quy trình xác thực riêng)
            if (newEmail != null && !newEmail.trim().isEmpty() && !newEmail.equals(user.getEmail())) {
                // *** CẦN KIỂM TRA EMAIL TRÙNG LẶP ***
                if (userService.existsByEmail(newEmail.trim())) {
                     redirectAttributes.addFlashAttribute("errorMessage", "Email đã được sử dụng.");
                     return "redirect:/user/profile";
                }
                user.setEmail(newEmail.trim());
                changed = true;
                // Cần gửi email xác thực email mới
            }
            */

            // Chỉ lưu nếu có thay đổi
            if (changed) {
                userService.save(user); // Lưu thay đổi vào User
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
            } else {
                 redirectAttributes.addFlashAttribute("infoMessage", "Không có thông tin nào thay đổi."); // Thông báo nếu không đổi gì
            }

            return "redirect:/user/profile";

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
            e.printStackTrace(); // Log lỗi
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật hồ sơ: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

     // === CÓ THỂ THÊM CÁC ENDPOINT KHÁC ===
     // Ví dụ: GET /user/profile/edit -> Trả về form edit chi tiết hơn
     //        POST /user/profile/change-password -> Xử lý đổi mật khẩu
}