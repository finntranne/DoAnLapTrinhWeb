package com.alotra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
	
	@GetMapping("/admin/login")
	public String getLogin() {
		return "admin/login";
	}
	
	@GetMapping("/admin/forgot")
	public String getForgot() {
		return "admin/forgot";
	}
	
	@GetMapping("/admin/dashboard")
	public String getDashboard() {
		return "admin/dashboard";
	}
	
	@GetMapping("/admin/orders")
	public String getOrderPage() {
		return "admin/orders";
	}
	
	@GetMapping("/admin/products")
	public String getProductPage() {
		return "admin/products";
	}
	
	
}
