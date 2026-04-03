package com.alotra.controller.vendor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.dto.shop.EmployeeSearchRequest;
import com.alotra.dto.shop.ShopEmployeeDTO;
import com.alotra.entity.user.User;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorStaffService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorStaffController {

    private final VendorStaffService vendorStaffService;

    private Integer getShopIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        Integer shopId = userDetails.getShopId();
        if (shopId == null) {
            throw new IllegalStateException("Ban chua dang ky shop. Vui long dang ky shop truoc.");
        }

        return shopId;
    }

    @GetMapping("/staff")
    public String listStaff(@AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Integer shopId = getShopIdOrThrow(userDetails);
            Pageable pageable = PageRequest.of(page, size);
            Page<ShopEmployeeDTO> employees = vendorStaffService.getShopEmployees(shopId, status, search, pageable);

            model.addAttribute("employees", employees);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", employees.getTotalPages());
            model.addAttribute("status", status);
            model.addAttribute("search", search);
            return "vendor/staff/list";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/shop/register";
        } catch (Exception e) {
            log.error("Error loading staff list", e);
            redirectAttributes.addFlashAttribute("error", "Co loi xay ra khi tai danh sach nhan vien.");
            return "redirect:/vendor/dashboard";
        }
    }

    @GetMapping("/staff/search")
    public String showSearchEmployeeForm(Model model) {
        model.addAttribute("searchRequest", new EmployeeSearchRequest());
        return "vendor/staff/search";
    }

    @PostMapping("/staff/search")
    @ResponseBody
    public ResponseEntity<?> searchUser(@AuthenticationPrincipal MyUserDetails userDetails,
            @Valid @RequestBody EmployeeSearchRequest request) {
        try {
            getShopIdOrThrow(userDetails);
            User user = vendorStaffService.searchUserForEmployee(request.getSearchTerm());

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("avatarURL", user.getAvatarURL());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/staff/add")
    public String addEmployee(@AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam Integer userId,
            @RequestParam Integer roleId,
            RedirectAttributes redirectAttributes) {
        try {
            Integer shopId = getShopIdOrThrow(userDetails);
            vendorStaffService.addEmployee(shopId, userId, roleId);
            redirectAttributes.addFlashAttribute("success", "Them nhan vien thanh cong!");
            return "redirect:/vendor/staff";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/shop/register";
        } catch (Exception e) {
            log.error("Error adding employee", e);
            redirectAttributes.addFlashAttribute("error", "Co loi xay ra: " + e.getMessage());
            return "redirect:/vendor/staff/search";
        }
    }

    @GetMapping("/staff/edit/{id}")
    public String showEditEmployeeForm(@AuthenticationPrincipal MyUserDetails userDetails,
            @PathVariable Integer id,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Integer shopId = getShopIdOrThrow(userDetails);
            ShopEmployeeDTO employee = vendorStaffService.getEmployeeDetail(shopId, id);
            model.addAttribute("employee", employee);
            return "vendor/staff/edit";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/shop/register";
        } catch (Exception e) {
            log.error("Error loading employee for edit", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/staff";
        }
    }

    @PostMapping("/staff/edit/{id}")
    public String updateEmployee(@AuthenticationPrincipal MyUserDetails userDetails,
            @PathVariable Integer id,
            @RequestParam Integer roleId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            Integer shopId = getShopIdOrThrow(userDetails);
            vendorStaffService.updateEmployee(shopId, id, roleId, status);
            redirectAttributes.addFlashAttribute("success", "Cap nhat thong tin nhan vien thanh cong!");
            return "redirect:/vendor/staff";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/shop/register";
        } catch (Exception e) {
            log.error("Error updating employee", e);
            redirectAttributes.addFlashAttribute("error", "Co loi xay ra: " + e.getMessage());
            return "redirect:/vendor/staff/edit/" + id;
        }
    }

    @PostMapping("/staff/deactivate/{id}")
    public String deactivateEmployee(@AuthenticationPrincipal MyUserDetails userDetails,
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            Integer shopId = getShopIdOrThrow(userDetails);
            vendorStaffService.deactivateEmployee(shopId, id);
            redirectAttributes.addFlashAttribute("success", "Da xoa nhan vien khoi cua hang!");
            return "redirect:/vendor/staff";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/shop/register";
        } catch (Exception e) {
            log.error("Error deactivating employee", e);
            redirectAttributes.addFlashAttribute("error", "Co loi xay ra: " + e.getMessage());
            return "redirect:/vendor/staff";
        }
    }
}
