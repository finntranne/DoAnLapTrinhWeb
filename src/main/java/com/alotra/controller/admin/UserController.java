package com.alotra.controller.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.model.user.UserModel;
import com.alotra.service.CloudinaryService;
import com.alotra.service.user.IRoleService;
import com.alotra.service.user.IUserService;

@Controller
@RequestMapping("admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
	
	@Autowired
	IUserService userService;
	
	@Autowired
	IRoleService roleService;
	
	@Autowired
	CloudinaryService cloudinary;
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	// ========== HIỂN THỊ DANH SÁCH ==========
	@GetMapping("")
	public String list(ModelMap model,
	                   @RequestParam(name = "page", defaultValue = "1") int page,
	                   @RequestParam(name = "size", defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page - 1, size);
		Page<User> userPage = userService.findAll(pageable);

		int totalPages = userPage.getTotalPages();

	    model.addAttribute("users", userPage.getContent());
	    model.addAttribute("currentPage", page);
	    model.addAttribute("totalPages", totalPages);
	    model.addAttribute("activeMenu", "users");

	    return "users/list";
	}

    // ========== THÊM MỚI USER ==========
    @GetMapping("/add")
    public String add(ModelMap model) {
        UserModel userModel = new UserModel();
        List<Role> roles = roleService.findAll();
        model.addAttribute("user", userModel);
        model.addAttribute("allRoles", roles);

        return "users/add";
    }

    
    @PostMapping("/save")
    public String saveUser(@ModelAttribute("user") UserModel userModel,
    		@RequestParam("roleId") Integer roleId) {
        User user = new User();
        BeanUtils.copyProperties(userModel, user);

        if (userModel.getFile() != null && !userModel.getFile().isEmpty()) {
            try {
            	String uploadedUrl = cloudinary.upload(userModel.getFile());
                user.setAvatar(uploadedUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Role role = roleService.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò có ID: " + roleId));
        user.setRole(role);
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.save(user);
        return "redirect:/admin/users";
    }
    
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Integer id, ModelMap model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + id));
        model.addAttribute("user", user);

        return "users/detail";
    }



    // ========== CHỈNH SỬA USER ==========
    @GetMapping("/edit/{id}")
    public String editUser(@PathVariable("id") Integer id, ModelMap model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Role> roles = roleService.findAll();
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roles);

        return "users/edit";
    }
    
    
    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable("id") Integer id,
            @ModelAttribute("user") User user,
            @RequestParam("avatarFile") MultipartFile avatarFile) throws IOException {

		User existingUser = userService.findById(id)
		.orElseThrow(() -> new IllegalArgumentException("User not found"));
		
		if (!avatarFile.isEmpty()) {
			if (existingUser.getAvatar() != null && !existingUser.getAvatar().isEmpty()) {
				cloudinary.delete(existingUser.getAvatar());
			}

			String uploadedUrl = cloudinary.upload(avatarFile);
			user.setAvatar(uploadedUrl);
		} else {
			user.setAvatar(existingUser.getAvatar());
		}

		user.setUser_id(existingUser.getUser_id());
		user.setPassword(existingUser.getPassword());
		user.setCreatedAt(existingUser.getCreatedAt());
		
		userService.save(user);
		return "redirect:/admin/users";
	}




    // ========== XÓA USER ==========
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id,
                         @RequestParam(value = "page", defaultValue = "1") int page,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Người dùng đã được xóa thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa người dùng thất bại: " + e.getMessage());
        }
        return "redirect:/admin/users?page=" + page;
    }


    // ========== TÌM KIẾM USER ==========
    @GetMapping("/search")
    public String searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            ModelMap model) {

        List<User> users = userService.searchUsers(username, email, roleId, status, startDate, endDate, page);
        int totalPages = userService.getTotalPages(username, email, roleId, status, startDate, endDate);

        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        model.addAttribute("roles", roleService.findAll());

        return "users/list";
    }


}
