package com.alotra.controller.vendor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.order.AuditLog;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorAuditLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor/audit-logs")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorAuditLogController {

    private final VendorAuditLogService vendorAuditLogService;

    @GetMapping
    public String listAuditLogs(@AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam(required = false) Integer orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Integer shopId = getShopIdOrThrow(userDetails);
            Pageable pageable = PageRequest.of(page, size);
            Page<AuditLog> auditLogs = vendorAuditLogService.getAuditLogs(shopId, orderId, pageable);

            model.addAttribute("auditLogs", auditLogs);
            model.addAttribute("orderIdFilter", orderId);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", auditLogs.getTotalPages());
            return "vendor/audit-logs/list";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/shop/register";
        } catch (Exception e) {
            log.error("Error loading audit logs", e);
            redirectAttributes.addFlashAttribute("error", "Khong the tai audit log.");
            return "redirect:/vendor/dashboard";
        }
    }

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
}
