package com.alotra.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alotra.service.order.OrderService;
import com.alotra.service.user.IUserService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminDashboardController {
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	IUserService userService;
	
	@GetMapping("/dashboard")
    public String adminDashboard(Model model) {
		
		BigDecimal totalRevenue = orderService.getTotalRevenueForCurrentMonth();
		double revenueChangeRate = orderService.calculateRevenueChangeRate(); 
		model.addAttribute("totalRevenue", totalRevenue);	 
	    model.addAttribute("revenueChangeRate", revenueChangeRate);
	    
	    long totalNewUsers = userService.getTotalNewUsersCurrentMonth();
	    double userChangeRate = userService.calculateUserChangeRate(); 
	    model.addAttribute("totalNewUsers", totalNewUsers);
	    model.addAttribute("userChangeRate", userChangeRate);
	    
	    long totalOrders = orderService.getTotalOrdersCurrentMonth();
	    double orderChangeRate = orderService.calculateOrderChangeRate();
	    model.addAttribute("totalOrders", totalOrders);
	    model.addAttribute("orderChangeRate", orderChangeRate);
	    
	    BigDecimal totalProfit = orderService.getTotalProfitCurrentMonth();
	    double profitChangeRate = orderService.calculateProfitChangeRate();	    
	    model.addAttribute("totalProfit", totalProfit);
	    model.addAttribute("profitChangeRate", profitChangeRate);
	    
	    List<Object[]> shopRanking = orderService.getMonthlyShopRanking();   
	    model.addAttribute("shopRanking", shopRanking);
	    
	    final int NUM_MONTHS = 7;
	    List<BigDecimal> monthlySalesData = orderService.getRecentMonthlySales(NUM_MONTHS);
	    List<String> monthlyLabels = orderService.getRecentMonthlyLabels(NUM_MONTHS);
	    
	    model.addAttribute("monthlySalesData", monthlySalesData);
	    model.addAttribute("monthlyLabels", monthlyLabels);
	    
        return "admin/dashboard";
    }

    // Thêm @ResponseBody để trả về text thay vì template
    @GetMapping("/test")
    @ResponseBody
    public String adminTest() {
        return "✅ Admin test page - if you see this, you have access!";
    }

}
