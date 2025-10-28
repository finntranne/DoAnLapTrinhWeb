package com.alotra.controller.admin;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.dto.user.UserModel;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.repository.user.RoleRepository;
import com.alotra.service.cloudinary.CloudinaryService;
import com.alotra.service.user.IUserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("admin/users")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {

	@Autowired
	IUserService userService;
	
	@Autowired
	RoleRepository roleRepository;
	
	@Autowired
	CloudinaryService cloudinary;
	
	@Autowired
	BCryptPasswordEncoder passwordEncoder;

	
	// ========== HIỂN THỊ DANH SÁCH ==========
	@GetMapping("")
	public String list(ModelMap model,
	                   @RequestParam(name = "page", defaultValue = "1") int page,
	                   @RequestParam(name = "size", defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page - 1, size);
		Page<User> userPage = userService.findAllWithoutAdmin(pageable);

		int totalPages = userPage.getTotalPages();
		
		List<Role> roles = roleRepository.findAllWithoutAdmin();
		model.addAttribute("roles", roles);

	    model.addAttribute("users", userPage.getContent());
	    model.addAttribute("currentPage", page);
	    model.addAttribute("totalPages", totalPages);
	    model.addAttribute("activeMenu", "users");

	    return "admin/users/list";
	}

    // ========== THÊM MỚI USER ==========
    @GetMapping("/add")
    public String add(ModelMap model) {
        User user = new User();
        List<Role> roles = roleRepository.findAllWithoutAdmin();
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roles);
        model.addAttribute("activeMenu", "users");
        return "admin/users/add";
    }

    
    @PostMapping("/save")
    public String saveUser(@ModelAttribute("user") UserModel userModel,
                           @RequestParam("roleId") Integer roleId,
                           RedirectAttributes redirectAttributes) {
        try {
        	
        	if (userService.existsByUsername(userModel.getUsername())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tên đăng nhập đã tồn tại!");
                return "redirect:/admin/users/add";
            }
        	if (userService.existsByEmail(userModel.getEmail())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email đã tồn tại!");
                return "redirect:/admin/users/add";
            }
        	if (userService.existsByPhoneNumber(userModel.getPhoneNumber())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại đã tồn tại!");
                return "redirect:/admin/users/add";
            }
        	
        	
        	
            User user = new User();

            BeanUtils.copyProperties(userModel, user);
            
            if (userModel.getFile() != null && !userModel.getFile().isEmpty()) {
                try {
                    // ⚙️ Gọi CloudinaryService để upload ảnh (an toàn hơn)
                    String uploadedUrl = cloudinary.uploadImage(userModel.getFile(), "avatars");
                    user.setAvatarURL(uploadedUrl);
                } catch (Exception e) {
                    log.error("Lỗi khi upload ảnh đại diện: {}", e.getMessage(), e);
                    redirectAttributes.addFlashAttribute("errorMessage", "Tải ảnh lên thất bại. Vui lòng thử lại!");
                    return "redirect:/admin/users/add";
                }
            }

            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò có ID: " + roleId));

            Set<Role> roles = new HashSet<>();
            roles.add(role);
            user.setRoles(roles);

			user.setPassword(passwordEncoder.encode(user.getPassword()));

            userService.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Người dùng mới đã được thêm thành công!");
            return "redirect:/admin/users";
            
        } catch (Exception e) {
        	log.error("Lỗi khi lưu người dùng: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi lưu người dùng!");
            return "redirect:/admin/users";
        }
    }


    
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Integer id, ModelMap model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + id));
        model.addAttribute("user", user);
        model.addAttribute("activeMenu", "users");
        return "admin/users/detail";
    }



    // ========== CHỈNH SỬA USER ==========
    @GetMapping("/edit/{id}")
    public String editUser(@PathVariable("id") Integer id, ModelMap model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Role> roles = roleRepository.findAllWithoutAdmin();
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roles);
        model.addAttribute("activeMenu", "users");
        return "admin/users/edit";
    }
    
    
    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable("id") Integer id,
                             @ModelAttribute("user") User user,
                             @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                             @RequestParam("roleId") Integer roleId,
                             RedirectAttributes redirectAttributes) {
        try {
            // 1️⃣ Tìm người dùng hiện có
            User existingUser = userService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + id));

            // 2️⃣ Xử lý avatar (upload + xóa ảnh cũ nếu có)
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    if (existingUser.getAvatarURL() != null && !existingUser.getAvatarURL().isEmpty()) {
                        String oldPublicId = cloudinary.extractPublicIdFromUrl(existingUser.getAvatarURL());
                        if (oldPublicId != null) {
                            cloudinary.deleteImage(oldPublicId);
                        }
                    }

                    String uploadedUrl = cloudinary.uploadImage(avatarFile, "avatars", existingUser.getId());
                    user.setAvatarURL(uploadedUrl);
                } catch (Exception e) {
                    log.error("Lỗi khi upload ảnh cho user {}: {}", existingUser.getUsername(), e.getMessage());
                    redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật ảnh đại diện. Vui lòng thử lại!");
                    return "redirect:/admin/users/edit/" + id;
                }
            } else {
                // Không upload mới → giữ ảnh cũ
                user.setAvatarURL(existingUser.getAvatarURL());
            }

            // 3️⃣ Giữ lại các thông tin không thay đổi
            user.setId(existingUser.getId());
            user.setCreatedAt(existingUser.getCreatedAt());
            user.setUpdatedAt(LocalDateTime.now());

            // 4️⃣ Xử lý mật khẩu: nếu để trống → giữ nguyên
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                user.setPassword(existingUser.getPassword());
            } else {
                // Người dùng nhập mật khẩu mới → mã hóa
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            // 5️⃣ Cập nhật vai trò
            Role selectedRole = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò!"));
            user.setRoles(Set.of(selectedRole));

            // 6️⃣ Lưu vào DB
            userService.save(user);
            log.info("Người dùng '{}' đã được cập nhật thành công.", user.getUsername());

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật người dùng thành công!");
            return "redirect:/admin/users";

        } catch (IllegalArgumentException e) {
            log.warn("Lỗi dữ liệu khi cập nhật user {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/edit/" + id;

        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi cập nhật user {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật người dùng!");
            return "redirect:/admin/users";
        }
    }







    // ========== XÓA USER ==========
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id,
                         @RequestParam(value = "page", defaultValue = "1") int page,
                         RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

            // ✅ Chuyển trạng thái thay vì xóa thật
            user.setStatus((byte) 0);
            userService.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Người dùng đã được tạm khóa thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tạm khóa người dùng thất bại: " + e.getMessage());
        }
        return "redirect:/admin/users?page=" + page;
    }



    // ========== TÌM KIẾM USER ==========
    @GetMapping("/search")
    public String searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            ModelMap model) {

        List<User> users = userService.searchUsers(username, email, phoneNumber, roleId, status, startDate, endDate, page);
        int totalPages = userService.getTotalPages(username, email, phoneNumber, roleId, status, startDate, endDate);

        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("phoneNumber", phoneNumber);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        model.addAttribute("roles", roleRepository.findAllWithoutAdmin());
        model.addAttribute("activeMenu", "users");
        return "admin/users/list";
    }

}
