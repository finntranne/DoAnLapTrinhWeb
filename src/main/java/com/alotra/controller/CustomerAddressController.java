package com.alotra.controller;


import com.alotra.entity.user.Address;
import com.alotra.entity.user.Customer;
import com.alotra.entity.user.User;
import com.alotra.repository.user.AddressRepository;
import com.alotra.service.user.CustomerService;
import com.alotra.service.user.UserService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/user/addresses") // Tiền tố chung cho quản lý địa chỉ
public class CustomerAddressController {

    @Autowired private AddressRepository customerAddressRepository;
    @Autowired private CustomerService customerService;
    
    @Autowired
    @Qualifier("userServiceImpl")
    private UserService userService;

    // === Hàm trợ giúp lấy Customer (Giống OrderController) ===
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
     * HIỂN THỊ DANH SÁCH ĐỊA CHỈ (SỔ ĐỊA CHỈ)
     * Xử lý GET /user/addresses
     */
    @GetMapping
    public String showAddressList(Model model) {
        try {
            Customer customer = getCurrentCustomer();
            List<Address> addresses = customerAddressRepository.findByCustomer(customer);
            model.addAttribute("addresses", addresses);
            return "user/address_list";
        } catch (ResponseStatusException e) {
            return "redirect:/login";
        }
    }

    /**
     * ENDPOINT ĐỂ HIỂN THỊ FORM (Sửa lỗi 404)
     * Xử lý GET /user/addresses/new
     */
    @GetMapping("/new")
    public String showAddAddressForm(Model model, @RequestParam(name = "origin", required = false, defaultValue = "address_list") String origin) {
        try {
            // Lấy customer để biết form này của ai (dù chưa dùng)
            getCurrentCustomer(); 
            
            // Tạo một đối tượng Address rỗng để binding với form
            Address newAddress = new Address();
            
            model.addAttribute("address", newAddress); // Đẩy object rỗng ra view
            model.addAttribute("pageTitle", "Thêm địa chỉ mới"); // Tiêu đề trang
            
            model.addAttribute("formAction", "/user/addresses/save");
            
            model.addAttribute("originUrl", "checkout".equals(origin) ? "/checkout" : "/user/addresses");
            model.addAttribute("origin", origin);
            
            return "user/address_form"; // Trả về file HTML (sẽ tạo ở Bước 2)

        } catch (ResponseStatusException e) {
            return "redirect:/login"; // Bắt đăng nhập
        }
    }

    /**
     * ENDPOINT ĐỂ LƯU FORM
     * Xử lý POST /user/addresses/save
     */
    @PostMapping("/save")
    public String saveAddress(@ModelAttribute("address") Address address, @RequestParam(name = "origin", required = false, defaultValue = "address_list") String origin,
            RedirectAttributes redirectAttributes) {
        
        try {
            Customer customer = getCurrentCustomer();
            
            // 1. Gán địa chỉ này cho khách hàng đang đăng nhập
            address.setCustomer(customer);

            // 2. Xử lý logic "isDefault" (quan trọng)
            if (address.isDefault()) {
                // Nếu người dùng tick "Đặt làm mặc định"
                // -> Bỏ tick "mặc định" ở tất cả các địa chỉ CŨ
                List<Address> allAddresses = customerAddressRepository.findByCustomer(customer);
                for (Address oldAddr : allAddresses) {
                    oldAddr.setDefault(false);
                }
                customerAddressRepository.saveAll(allAddresses);
            }
            
            // 3. Lưu địa chỉ MỚI (đã có isDefault = true)
            customerAddressRepository.save(address);

            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu địa chỉ thành công!");

            // === SỬA REDIRECT Ở ĐÂY ===
            String redirectUrl = "checkout".equals(origin) ? "/checkout" : "/user/addresses";
            return "redirect:" + redirectUrl;

        } catch (ResponseStatusException e) {
            return "redirect:/login"; // Lỗi xác thực
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/user/addresses/new?error=true&origin=" + origin;
        }
    }
    
    /**
     * === HÀM MỚI: XÓA ĐỊA CHỈ ===
     * Xử lý POST /user/addresses/delete/{id}
     */
    @PostMapping("/delete/{id}")
    public String deleteAddress(@PathVariable("id") Integer addressId, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = getCurrentCustomer();
            Address addressToDelete = customerAddressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ."));

            // Kiểm tra bảo mật: địa chỉ này phải của user hiện tại
            if (!addressToDelete.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                throw new AccessDeniedException("Bạn không có quyền xóa địa chỉ này.");
            }

            // Kiểm tra: Không cho xóa địa chỉ mặc định cuối cùng (nếu chỉ còn 1 cái)
             List<Address> allAddresses = customerAddressRepository.findByCustomer(customer);
             if (addressToDelete.isDefault() && allAddresses.size() <= 1) {
                  redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa địa chỉ mặc định duy nhất.");
                  return "redirect:/user/addresses";
             }

            customerAddressRepository.delete(addressToDelete);

            // Nếu vừa xóa địa chỉ mặc định, chọn cái khác làm mặc định (nếu còn)
            if (addressToDelete.isDefault() && allAddresses.size() > 1) {
                 Address newDefault = customerAddressRepository.findByCustomer(customer).stream().findFirst().orElse(null);
                 if (newDefault != null) {
                     newDefault.setDefault(true);
                     customerAddressRepository.save(newDefault);
                 }
            }


            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa địa chỉ thành công!");

        } catch (ResponseStatusException e) { return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa địa chỉ.");
        }
        return "redirect:/user/addresses";
    }

    /**
     * === HÀM MỚI: ĐẶT LÀM MẶC ĐỊNH ===
     * Xử lý POST /user/addresses/set-default/{id}
     */
    @PostMapping("/set-default/{id}")
    public String setDefaultAddress(@PathVariable("id") Integer addressId, RedirectAttributes redirectAttributes) {
         try {
            Customer customer = getCurrentCustomer();
            Address newDefaultAddress = customerAddressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ."));

            // Kiểm tra bảo mật
            if (!newDefaultAddress.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                throw new AccessDeniedException("Bạn không có quyền sửa địa chỉ này.");
            }

            // Bỏ mặc định tất cả địa chỉ khác
            List<Address> allAddresses = customerAddressRepository.findByCustomer(customer);
            for (Address addr : allAddresses) {
                 addr.setDefault(false);
            }
            // Đặt mặc định cho địa chỉ được chọn
            newDefaultAddress.setDefault(true);

            // Lưu tất cả thay đổi
            customerAddressRepository.saveAll(allAddresses);
            // Hoặc chỉ lưu newDefaultAddress nếu bạn không muốn duyệt lại list
            // customerAddressRepository.save(newDefaultAddress);


            redirectAttributes.addFlashAttribute("successMessage", "Đã đặt địa chỉ làm mặc định!");

        } catch (ResponseStatusException e) { return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi đặt địa chỉ mặc định.");
        }
        return "redirect:/user/addresses";
    }

    /**
     * === HÀM MỚI: HIỂN THỊ FORM SỬA ===
     * Xử lý GET /user/addresses/edit/{id}
     */
    @GetMapping("/edit/{id}")
    public String showEditAddressForm(@PathVariable("id") Integer addressId, Model model) {
        try {
            Customer customer = getCurrentCustomer();
            Address addressToEdit = customerAddressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ."));

            // Kiểm tra bảo mật
            if (!addressToEdit.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                throw new AccessDeniedException("Bạn không có quyền sửa địa chỉ này.");
            }

            model.addAttribute("address", addressToEdit); // <-- Đẩy địa chỉ CÓ DỮ LIỆU ra view
            model.addAttribute("pageTitle", "Chỉnh sửa địa chỉ"); // Đổi tiêu đề
            model.addAttribute("formAction", "/user/addresses/update"); // Action cho form SỬA
            model.addAttribute("originUrl", "/user/addresses"); // Nút Hủy luôn về danh sách

            return "user/address_form"; // Vẫn dùng chung form view

        } catch (ResponseStatusException e) { return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            // Có thể thêm redirectAttributes để báo lỗi
            return "redirect:/user/addresses";
        }
    }


    /**
     * === HÀM MỚI: XỬ LÝ CẬP NHẬT ĐỊA CHỈ ===
     * Xử lý POST /user/addresses/update
     */
    @PostMapping("/update")
    public String updateAddress(@ModelAttribute("address") Address updatedAddress, // Dữ liệu mới từ form
                                RedirectAttributes redirectAttributes) {
        try {
            Customer customer = getCurrentCustomer();

            // 1. Lấy địa chỉ GỐC từ CSDL để đảm bảo ID đúng và thuộc về user
            Address existingAddress = customerAddressRepository.findById(updatedAddress.getAddressID())
                    .orElseThrow(() -> new EntityNotFoundException("Địa chỉ không tồn tại để cập nhật."));

            // 2. Kiểm tra bảo mật
            if (!existingAddress.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                throw new AccessDeniedException("Bạn không có quyền sửa địa chỉ này.");
            }

            // 3. Cập nhật các trường từ dữ liệu form vào địa chỉ gốc
            existingAddress.setRecipientName(updatedAddress.getRecipientName());
            existingAddress.setPhoneNumber(updatedAddress.getPhoneNumber());
            existingAddress.setFullAddress(updatedAddress.getFullAddress());
            existingAddress.setAddressName(updatedAddress.getAddressName());
            // KHÔNG cập nhật isDefault ở đây, dùng hàm /set-default riêng

            // 4. Lưu lại địa chỉ đã cập nhật
            customerAddressRepository.save(existingAddress);

            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật địa chỉ thành công!");
            return "redirect:/user/addresses"; // Về trang danh sách

        } catch (ResponseStatusException e) { return "redirect:/login";
        } catch (EntityNotFoundException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/addresses";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật địa chỉ.");
            // Quay lại form edit nếu lỗi (cần truyền ID)
            return "redirect:/user/addresses/edit/" + updatedAddress.getAddressID() + "?error=true";
        }
    }
}
