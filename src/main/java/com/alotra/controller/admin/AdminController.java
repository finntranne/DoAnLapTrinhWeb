package com.alotra.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    // Thêm @ResponseBody để trả về text thay vì template
    @GetMapping("/test")
    @ResponseBody
    public String adminTest() {
        return "✅ Admin test page - if you see this, you have access!";
    }
}