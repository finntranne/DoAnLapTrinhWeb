package com.alotra.controller;

import com.alotra.entity.user.Address;
import com.alotra.entity.user.Customer;
import com.alotra.entity.user.User;
import com.alotra.service.user.CustomerService;
import com.alotra.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/profile")
public class UserProfileController {

    @Autowired 
    private CustomerService customerService;

    @Autowired
    @Qualifier("userServiceImpl")
    private UserService userService;

    /**
     * Lấy thông tin Customer hiện tại từ Security Context
     */
    private Customer getCurrentCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        String username = auth.getName();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
        return customerService.findByUser(currentUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ khách hàng"));
    }

    /**
     * Hiển thị trang hồ sơ cá nhân
     * GET /user/profile
     */
    @GetMapping
    public String showProfilePage(Model model) {
        try {
            Customer customer = getCurrentCustomer();
            
            // Lấy địa chỉ mặc định nếu có
            Address defaultAddress = customer.getAddresses() != null 
                ? customer.getAddresses().stream()
                    .filter(Address::isDefault)
                    .findFirst()
                    .orElse(null)
                : null;
            
            model.addAttribute("customer", customer);
            model.addAttribute("defaultAddress", defaultAddress);
            model.addAttribute("totalAddresses", customer.getAddresses() != null ? customer.getAddresses().size() : 0);
            
            return "user/profile";
            
        } catch (ResponseStatusException e) {
            return "redirect:/login";
        }
    }

    /**
     * Cập nhật thông tin cá nhân
     * POST /user/profile/update
     */
    @PostMapping("/update")
    public String updateProfile(@RequestParam("fullName") String newFullName,
                                @RequestParam(value = "email", required = false) String newEmail,
                                RedirectAttributes redirectAttributes) {
        try {
            Customer customer = getCurrentCustomer();
            User user = customer.getUser();

            // Validate fullName
            if (newFullName == null || newFullName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Họ và tên không được để trống!");
                return "redirect:/user/profile";
            }

            // Cập nhật thông tin User
            user.setFullname(newFullName.trim());
            
            // Cập nhật thông tin Customer
            customer.setFullName(newFullName.trim());
            
            // Cập nhật email nếu có thay đổi và không trùng
            if (newEmail != null && !newEmail.trim().isEmpty() && !newEmail.equals(customer.getEmail())) {
                customer.setEmail(newEmail.trim());
            }

            // Lưu thay đổi
            userService.save(user);
            customerService.save(customer);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
            return "redirect:/user/profile";

        } catch (ResponseStatusException e) {
            return "redirect:/login";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật hồ sơ: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }
}