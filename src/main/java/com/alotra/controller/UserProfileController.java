package com.alotra.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.dto.auth.ChangePasswordDto;
import com.alotra.entity.location.Address;
import com.alotra.entity.user.User;
import com.alotra.repository.location.AddressRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.shop.StoreService;
import com.alotra.service.user.UserService;
import com.alotra.view.address.AddressDisplayView;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/user/profile")
public class UserProfileController {

    @Autowired
    private UserService userService;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private StoreService storeService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap.");
        }

        String username = auth.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung: " + username));
    }

    private int getCurrentCartItemCount() {
        try {
            return cartService.getCartItemCount(getCurrentAuthenticatedUser());
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return 0;
        }
    }

    private Integer getSelectedShopId(HttpSession session) {
        Integer selectedShopId = (Integer) session.getAttribute("selectedShopId");
        return selectedShopId == null ? 0 : selectedShopId;
    }

    @GetMapping
    public String showProfilePage(Model model, HttpSession session) {
        try {
            Integer selectedShopId = getSelectedShopId(session);
            User user = getCurrentAuthenticatedUser();

            List<Address> addresses = addressRepository.findByUserId(user.getId());
            Address defaultAddress = addresses.stream()
                    .filter(address -> Boolean.TRUE.equals(address.getIsDefault()))
                    .findFirst()
                    .orElse(null);

            model.addAttribute("customer", user);
            model.addAttribute("defaultAddress",
                    defaultAddress != null ? new AddressDisplayView(defaultAddress, user) : null);
            model.addAttribute("totalAddresses", addresses.size());
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
            return "user/profile";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        }
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam("fullName") String newFullName,
            @RequestParam(value = "phoneNumber", required = false) String newPhoneNumber,
            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser();
            if (newFullName == null || newFullName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ho va ten khong duoc de trong!");
                return "redirect:/user/profile";
            }

            boolean changed = false;
            if (!newFullName.trim().equals(user.getFullName())) {
                user.setFullName(newFullName.trim());
                changed = true;
            }

            if (newPhoneNumber != null && !newPhoneNumber.trim().isEmpty()
                    && (user.getPhoneNumber() == null || !newPhoneNumber.trim().equals(user.getPhoneNumber()))) {
                Optional<User> existingUser = userService.findByPhoneNumber(newPhoneNumber.trim());
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "So dien thoai da duoc su dung boi tai khoan khac.");
                    return "redirect:/user/profile";
                }

                user.setPhoneNumber(newPhoneNumber.trim());
                changed = true;
            }

            if (changed) {
                userService.save(user);
                redirectAttributes.addFlashAttribute("successMessage", "Cap nhat ho so thanh cong!");
            }
            return "redirect:/user/profile";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loi khi cap nhat ho so: " + ex.getMessage());
            return "redirect:/user/profile";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model, HttpSession session) {
        try {
            getCurrentAuthenticatedUser();

            Integer selectedShopId = getSelectedShopId(session);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

            if (!model.containsAttribute("changePasswordDto")) {
                model.addAttribute("changePasswordDto", new ChangePasswordDto());
            }
            return "user/change-password";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        }
    }

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute("changePasswordDto") @Valid ChangePasswordDto changePasswordDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            return "user/change-password";
        }

        try {
            User user = getCurrentAuthenticatedUser();

            if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
                model.addAttribute("passwordMismatchError",
                        "Xac nhan mat khau khong khop voi mat khau moi.");
                model.addAttribute("cartItemCount", getCurrentCartItemCount());
                model.addAttribute("categories", categoryService.findAll());
                return "user/change-password";
            }

            if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mat khau hien tai khong dung.");
                redirectAttributes.addFlashAttribute("changePasswordDto", changePasswordDto);
                redirectAttributes.addFlashAttribute(
                        BindingResult.MODEL_KEY_PREFIX + "changePasswordDto",
                        bindingResult);
                return "redirect:/user/profile/change-password";
            }

            user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
            userService.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Doi mat khau thanh cong!");
            return "redirect:/user/profile";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loi he thong khi doi mat khau.");
            return "redirect:/user/profile/change-password";
        }
    }
}
