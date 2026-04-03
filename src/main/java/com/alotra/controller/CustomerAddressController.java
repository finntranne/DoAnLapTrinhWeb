

package com.alotra.controller; // Giữ package này

// Import các entity đã merge
import com.alotra.entity.location.Address;
import com.alotra.entity.user.User; // Sử dụng User

// Import Service và Repository đã merge
import com.alotra.repository.location.AddressRepository; // Sử dụng AddressRepository đã sửa
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.shop.StoreService;
import com.alotra.service.user.UserService; // Sử dụng UserService

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
// Bỏ import Qualifier nếu không cần thiết
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Thêm exception này
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; // Sử dụng * cho gọn
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user/addresses") // Giữ tiền tố chung
public class CustomerAddressController { // Tên class có thể giữ nguyên hoặc đổi thành UserAddressController

    // *** SỬA: Dùng AddressRepository đã merge ***
    @Autowired private AddressRepository addressRepository;
    // @Autowired private CustomerService customerService; // *** BỎ CustomerService ***

    // @Autowired @Qualifier("userServiceImpl") // Qualifier có thể không cần
    @Autowired private UserService userService; // *** GIỮ UserService ***

    @Autowired private CartService cartService;
    @Autowired private CategoryService categoryService;
    @Autowired private StoreService storeService;

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

    // --- Hàm trợ giúp lấy số lượng giỏ hàng (Giống CartController, ĐÃ SỬA) ---
    private int getCurrentCartItemCount() {
        try {
            User user = getCurrentAuthenticatedUser();
            return cartService.getCartItemCount(user); // Gọi service với User
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return 0; // Trả về 0 nếu chưa đăng nhập hoặc có lỗi
        }
    }
    private Integer getSelectedShopId(HttpSession session) {
        Integer selectedShopId = (Integer) session.getAttribute("selectedShopId");
        return (selectedShopId == null) ? 0 : selectedShopId; // Mặc định là 0 (Xem tất cả)
    }

    /**
     * HIỂN THỊ DANH SÁCH ĐỊA CHỈ (ĐÃ SỬA)
     * Xử lý GET /user/addresses
     */
    @GetMapping
    public String showAddressList(Model model, HttpSession session) {
        try {
        	Integer selectedShopId = getSelectedShopId(session);
            User user = getCurrentAuthenticatedUser(); // Lấy User
            // *** SỬA: Gọi repository với User ID ***
            List<Address> addresses = addressRepository.findByUserId(user.getId());
            model.addAttribute("addresses", addresses);
            model.addAttribute("cartItemCount", getCurrentCartItemCount()); // Đã sửa
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
            return "user/address_list";
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        }
    }

    /**
     * HIỂN THỊ FORM THÊM MỚI (ĐÃ SỬA)
     * Xử lý GET /user/addresses/new
     */
    @GetMapping("/new")
    public String showAddAddressForm(Model model, @RequestParam(name = "origin", required = false, defaultValue = "address_list") String origin, HttpSession session) {
        try {
        	Integer selectedShopId = getSelectedShopId(session);
            getCurrentAuthenticatedUser(); // Chỉ cần kiểm tra đăng nhập
            Address newAddress = new Address();
            model.addAttribute("address", newAddress);
            model.addAttribute("pageTitle", "Thêm địa chỉ mới");
            model.addAttribute("formAction", "/user/addresses/save"); // Action lưu mới
            model.addAttribute("originUrl", "checkout".equals(origin) ? "/checkout" : "/user/addresses");
            model.addAttribute("origin", origin);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
            return "user/address_form";
        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        }
    }

    /**
     * LƯU ĐỊA CHỈ MỚI (ĐÃ SỬA)
     * Xử lý POST /user/addresses/save
     */
    @PostMapping("/save")
    public String saveAddress(@ModelAttribute("address") Address address,
                              @RequestParam(name = "origin", required = false, defaultValue = "address_list") String origin,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser(); // Lấy User
            // address.setUser(user); // *** SỬA: Gán User ***

            if (address.getIsDefault()) {
                // Bỏ tick "mặc định" ở tất cả các địa chỉ CŨ của User
                // *** SỬA: Gọi repository với User ID ***
                List<Address> allAddresses = addressRepository.findByUserId(user.getId());
                for (Address oldAddr : allAddresses) {
                    // Tránh cập nhật chính địa chỉ đang thêm (nếu nó là cái đầu tiên)
                    if (oldAddr.getAddressID() != null) {
                         oldAddr.setIsDefault(false);
                    }
                }
                 // Lưu thay đổi trạng thái mặc định của các địa chỉ cũ
                addressRepository.saveAll(allAddresses);
            }

            // Lưu địa chỉ MỚI (có thể đã set isDefault = true)
            addressRepository.save(address);

            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu địa chỉ thành công!");
            String redirectUrl = "checkout".equals(origin) ? "/checkout" : "/user/addresses";
            return "redirect:" + redirectUrl;

        } catch (ResponseStatusException | UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
            e.printStackTrace(); // Log lỗi
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu địa chỉ: " + e.getMessage());
            // Redirect lại form mới kèm lỗi
            return "redirect:/user/addresses/new?error=true&origin=" + origin;
        }
    }

    /**
     * XÓA ĐỊA CHỈ (ĐÃ SỬA)
     * Xử lý POST /user/addresses/delete/{id}
     */
    @PostMapping("/delete/{id}")
    public String deleteAddress(@PathVariable("id") Integer addressId, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser(); // Lấy User
            Address addressToDelete = addressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ."));

            // Kiểm tra bảo mật: địa chỉ này phải của user hiện tại
            // *** SỬA: So sánh User ID ***
//            if (!addressToDelete.getUser().getId().equals(user.getId())) {
//                throw new AccessDeniedException("Bạn không có quyền xóa địa chỉ này.");
//            }

            // Lấy danh sách địa chỉ trước khi xóa
            List<Address> allAddresses = addressRepository.findByUserId(user.getId());

            // Kiểm tra: Không cho xóa địa chỉ mặc định duy nhất
             if (addressToDelete.getIsDefault() && allAddresses.size() <= 1) {
                  redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa địa chỉ mặc định duy nhất.");
                  return "redirect:/user/addresses";
             }

            addressRepository.delete(addressToDelete);

            // Nếu vừa xóa địa chỉ mặc định và còn địa chỉ khác, chọn cái khác làm mặc định
            if (addressToDelete.getIsDefault() && allAddresses.size() > 1) {
                 // Lấy lại danh sách sau khi xóa HOẶC tìm trong danh sách cũ trừ cái vừa xóa
                 Address newDefault = addressRepository.findByUserId(user.getId()).stream().findFirst().orElse(null);
                 if (newDefault != null) {
                     newDefault.setIsDefault(true);
                     addressRepository.save(newDefault);
                 }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa địa chỉ thành công!");

        } catch (ResponseStatusException | UsernameNotFoundException e) { return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // Log lỗi
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa địa chỉ.");
        }
        return "redirect:/user/addresses";
    }

    /**
     * ĐẶT LÀM MẶC ĐỊNH (ĐÃ SỬA)
     * Xử lý POST /user/addresses/set-default/{id}
     */
    @PostMapping("/set-default/{id}")
    public String setDefaultAddress(@PathVariable("id") Integer addressId, RedirectAttributes redirectAttributes) {
         try {
            User user = getCurrentAuthenticatedUser(); // Lấy User
            Address newDefaultAddress = addressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ."));

            // Kiểm tra bảo mật
            // *** SỬA: So sánh User ID ***
//            if (!newDefaultAddress.getUser().getId().equals(user.getId())) {
//                throw new AccessDeniedException("Bạn không có quyền sửa địa chỉ này.");
//            }

            // Bỏ mặc định tất cả địa chỉ khác của User
            // *** SỬA: Gọi repository với User ID ***
            List<Address> allAddresses = addressRepository.findByUserId(user.getId());
            for (Address addr : allAddresses) {
                 // Chỉ bỏ tick nếu nó không phải là cái đang được chọn làm mặc định
                 if (!addr.getAddressID().equals(addressId)) {
                     addr.setIsDefault(false);
                 }
            }
            // Đặt mặc định cho địa chỉ được chọn
            newDefaultAddress.setIsDefault(true);

            // Lưu tất cả thay đổi
            addressRepository.saveAll(allAddresses);

            redirectAttributes.addFlashAttribute("successMessage", "Đã đặt địa chỉ làm mặc định!");

        } catch (ResponseStatusException | UsernameNotFoundException e) { return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // Log lỗi
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi đặt địa chỉ mặc định.");
        }
        return "redirect:/user/addresses";
    }

    /**
     * HIỂN THỊ FORM SỬA (ĐÃ SỬA)
     * Xử lý GET /user/addresses/edit/{id}
     */
    @GetMapping("/edit/{id}")
    public String showEditAddressForm(@PathVariable("id") Integer addressId, Model model, HttpSession session) {
        try {
        	Integer selectedShopId = getSelectedShopId(session);
            User user = getCurrentAuthenticatedUser(); // Lấy User
            Address addressToEdit = addressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ."));

            // Kiểm tra bảo mật
            // *** SỬA: So sánh User ID ***
//            if (!addressToEdit.getUser().getId().equals(user.getId())) {
//                throw new AccessDeniedException("Bạn không có quyền sửa địa chỉ này.");
//            }

            model.addAttribute("address", addressToEdit);
            model.addAttribute("pageTitle", "Chỉnh sửa địa chỉ");
            model.addAttribute("formAction", "/user/addresses/update"); // Action cập nhật
            model.addAttribute("originUrl", "/user/addresses");
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

            return "user/address_form"; // Vẫn dùng chung form

        } catch (ResponseStatusException | UsernameNotFoundException e) { return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            // Thêm redirectAttributes để báo lỗi rõ hơn
            // redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/addresses"; // Về danh sách nếu lỗi
        }
    }


    /**
     * XỬ LÝ CẬP NHẬT ĐỊA CHỈ (ĐÃ SỬA)
     * Xử lý POST /user/addresses/update
     */
//    @PostMapping("/update")
//    public String updateAddress(@ModelAttribute("address") Address updatedAddress,
//                                RedirectAttributes redirectAttributes) {
//        try {
//            User user = getCurrentAuthenticatedUser(); // Lấy User
//
//            // 1. Lấy địa chỉ GỐC từ CSDL bằng ID từ form
//            if (updatedAddress.getAddressID() == null) {
//                 throw new IllegalArgumentException("Thiếu ID địa chỉ để cập nhật.");
//            }
//            Address existingAddress = addressRepository.findById(updatedAddress.getAddressID())
//                    .orElseThrow(() -> new EntityNotFoundException("Địa chỉ không tồn tại để cập nhật."));
//
//            // 2. Kiểm tra bảo mật
//            // *** SỬA: So sánh User ID ***
//            if (!existingAddress.getUser().getId().equals(user.getId())) {
//                throw new AccessDeniedException("Bạn không có quyền sửa địa chỉ này.");
//            }
//
//            // 3. Cập nhật các trường (KHÔNG cập nhật isDefault và User ở đây)
//            existingAddress.setRecipientName(updatedAddress.getRecipientName());
//            existingAddress.setPhoneNumber(updatedAddress.getPhoneNumber());
//            existingAddress.setFullAddress(updatedAddress.getFullAddress());
//            existingAddress.setAddressName(updatedAddress.getAddressName());
//            // existingAddress.setDefault(updatedAddress.isDefault()); // Dùng /set-default riêng
//
//            // 4. Lưu lại
//            addressRepository.save(existingAddress);
//
//            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật địa chỉ thành công!");
//            return "redirect:/user/addresses";
//
//        } catch (ResponseStatusException | UsernameNotFoundException e) { return "redirect:/login";
//        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//            // Nếu lỗi liên quan đến ID, quay về danh sách
//            if (updatedAddress.getAddressID() == null) return "redirect:/user/addresses";
//            // Nếu lỗi khác, quay về form edit
//            return "redirect:/user/addresses/edit/" + updatedAddress.getAddressID() + "?error=true";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log lỗi
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật địa chỉ.");
//             if (updatedAddress.getAddressID() == null) return "redirect:/user/addresses";
//            return "redirect:/user/addresses/edit/" + updatedAddress.getAddressID() + "?error=true";
//        }
//    }
}