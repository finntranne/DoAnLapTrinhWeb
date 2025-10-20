package com.alotra.controller.vendor;

import com.alotra.dto.*;
import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.dto.product.ProductStatisticsDTO;
import com.alotra.dto.promotion.PromotionRequestDTO;
import com.alotra.dto.response.ApprovalResponseDTO;
import com.alotra.dto.shop.ShopDashboardDTO;
import com.alotra.dto.shop.ShopOrderDTO;
import com.alotra.dto.shop.ShopRevenueDTO;
import com.alotra.entity.*;
import com.alotra.entity.order.Order;
import com.alotra.service.vendor.VendorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorController {
    
    private final VendorService vendorService;
    
    // ==================== DASHBOARD ====================
    
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        Integer shopId = userPrincipal.getShopId();
        
        ShopDashboardDTO dashboard = vendorService.getShopDashboard(shopId);
        model.addAttribute("dashboard", dashboard);
        
        // Lấy pending approvals
        List<ApprovalResponseDTO> pendingApprovals = vendorService.getPendingApprovals(shopId);
        model.addAttribute("pendingApprovals", pendingApprovals);
        
        return "vendor/dashboard";
    }
    
    // ==================== PRODUCT MANAGEMENT ====================
    
    @GetMapping("/products")
    public String listProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Byte status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        Integer shopId = userPrincipal.getShopId();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<ProductStatisticsDTO> products = vendorService.getShopProducts(shopId, status, search, pageable);
        
        model.addAttribute("products", products);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("status", status);
        model.addAttribute("search", search);
        
        return "vendor/products/list";
    }
    
    @GetMapping("/products/create")
    public String showCreateProductForm(Model model) {
        model.addAttribute("product", new ProductRequestDTO());
        model.addAttribute("action", "create");
        return "vendor/products/form";
    }
    
    @PostMapping("/products/create")
    public String createProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @ModelAttribute("product") ProductRequestDTO request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "vendor/products/form";
        }
        
        try {
            Integer shopId = userPrincipal.getShopId();
            Integer userId = userPrincipal.getUserId();
            
            vendorService.requestProductCreation(shopId, request, userId);
            
            redirectAttributes.addFlashAttribute("success", 
                "Yêu cầu tạo sản phẩm đã được gửi. Vui lòng chờ admin phê duyệt.");
            
            return "redirect:/vendor/products";
            
        } catch (JsonProcessingException e) {
            log.error("Error creating product", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tạo sản phẩm");
            return "redirect:/vendor/products/create";
        }
    }
    
    @GetMapping("/products/edit/{id}")
    public String showEditProductForm(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer id,
            Model model) {
        
        Integer shopId = userPrincipal.getShopId();
        
        // Load product data and populate form
        // ... implementation to load product
        
        model.addAttribute("product", new ProductRequestDTO());
        model.addAttribute("action", "edit");
        model.addAttribute("productId", id);
        
        return "vendor/products/form";
    }
    
    @PostMapping("/products/edit/{id}")
    public String updateProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer id,
            @Valid @ModelAttribute("product") ProductRequestDTO request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "vendor/products/form";
        }
        
        try {
            Integer shopId = userPrincipal.getShopId();
            Integer userId = userPrincipal.getUserId();
            
            request.setProductId(id);
            vendorService.requestProductUpdate(shopId, request, userId);
            
            redirectAttributes.addFlashAttribute("success", 
                "Yêu cầu cập nhật sản phẩm đã được gửi. Vui lòng chờ admin phê duyệt.");
            
            return "redirect:/vendor/products";
            
        } catch (Exception e) {
            log.error("Error updating product", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/products/edit/" + id;
        }
    }
    
    @PostMapping("/products/delete/{id}")
    public String deleteProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        
        try {
            Integer shopId = userPrincipal.getShopId();
            Integer userId = userPrincipal.getUserId();
            
            vendorService.requestProductDeletion(shopId, id, userId);
            
            redirectAttributes.addFlashAttribute("success", 
                "Yêu cầu xóa sản phẩm đã được gửi. Vui lòng chờ admin phê duyệt.");
            
        } catch (Exception e) {
            log.error("Error deleting product", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/vendor/products";
    }
    
    // ==================== PROMOTION MANAGEMENT ====================
    
    @GetMapping("/promotions")
    public String listPromotions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Byte status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        Integer shopId = userPrincipal.getShopId();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Promotion> promotions = vendorService.getShopPromotions(shopId, status, pageable);
        
        model.addAttribute("promotions", promotions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", promotions.getTotalPages());
        model.addAttribute("status", status);
        
        return "vendor/promotions/list";
    }
    
    @GetMapping("/promotions/create")
    public String showCreatePromotionForm(Model model) {
        model.addAttribute("promotion", new PromotionRequestDTO());
        model.addAttribute("action", "create");
        return "vendor/promotions/form";
    }
    
    @PostMapping("/promotions/create")
    public String createPromotion(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @ModelAttribute("promotion") PromotionRequestDTO request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "vendor/promotions/form";
        }
        
        try {
            Integer shopId = userPrincipal.getShopId();
            Integer userId = userPrincipal.getUserId();
            
            vendorService.requestPromotionCreation(shopId, request, userId);
            
            redirectAttributes.addFlashAttribute("success", 
                "Yêu cầu tạo khuyến mãi đã được gửi. Vui lòng chờ admin phê duyệt.");
            
            return "redirect:/vendor/promotions";
            
        } catch (JsonProcessingException e) {
            log.error("Error creating promotion", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tạo khuyến mãi");
            return "redirect:/vendor/promotions/create";
        }
    }
    
    @PostMapping("/promotions/edit/{id}")
    public String updatePromotion(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer id,
            @Valid @ModelAttribute("promotion") PromotionRequestDTO request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "vendor/promotions/form";
        }
        
        try {
            Integer shopId = userPrincipal.getShopId();
            Integer userId = userPrincipal.getUserId();
            
            request.setPromotionId(id);
            vendorService.requestPromotionUpdate(shopId, request, userId);
            
            redirectAttributes.addFlashAttribute("success", 
                "Yêu cầu cập nhật khuyến mãi đã được gửi. Vui lòng chờ admin phê duyệt.");
            
            return "redirect:/vendor/promotions";
            
        } catch (Exception e) {
            log.error("Error updating promotion", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/promotions";
        }
    }
    
    @PostMapping("/promotions/delete/{id}")
    public String deletePromotion(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        
        try {
            Integer shopId = userPrincipal.getShopId();
            Integer userId = userPrincipal.getUserId();
            
            vendorService.requestPromotionDeletion(shopId, id, userId);
            
            redirectAttributes.addFlashAttribute("success", 
                "Yêu cầu xóa khuyến mãi đã được gửi. Vui lòng chờ admin phê duyệt.");
            
        } catch (Exception e) {
            log.error("Error deleting promotion", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/vendor/promotions";
    }
    
    // ==================== ORDER MANAGEMENT ====================
    
    @GetMapping("/orders")
    public String listOrders(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        Integer shopId = userPrincipal.getShopId();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<ShopOrderDTO> orders = vendorService.getShopOrders(shopId, status, pageable);
        
        model.addAttribute("orders", orders);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        model.addAttribute("status", status);
        
        return "vendor/orders/list";
    }
    
    @GetMapping("/orders/{id}")
    public String viewOrderDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer id,
            Model model) {
        
        Integer shopId = userPrincipal.getShopId();
        Order order = vendorService.getOrderDetail(shopId, id);
        
        model.addAttribute("order", order);
        
        return "vendor/orders/detail";
    }
    
    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer id,
            @RequestParam String newStatus,
            RedirectAttributes redirectAttributes) {
        
        try {
            Integer shopId = userPrincipal.getShopId();
            Integer userId = userPrincipal.getUserId();
            
            vendorService.updateOrderStatus(shopId, id, newStatus, userId);
            
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái đơn hàng thành công");
            
        } catch (Exception e) {
            log.error("Error updating order status", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/vendor/orders/" + id;
    }
    
    // ==================== REVENUE MANAGEMENT ====================
    
    @GetMapping("/revenue")
    public String viewRevenue(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model) {
        
        Integer shopId = userPrincipal.getShopId();
        
        List<ShopRevenueDTO> revenues = vendorService.getShopRevenue(shopId, startDate, endDate);
        
        model.addAttribute("revenues", revenues);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        return "vendor/revenue";
    }
    
    // ==================== APPROVAL STATUS ====================
    
    @GetMapping("/approvals")
    public String viewApprovals(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Model model) {
        
        Integer shopId = userPrincipal.getShopId();
        
        List<ApprovalResponseDTO> approvals = vendorService.getPendingApprovals(shopId);
        
        model.addAttribute("approvals", approvals);
        
        return "vendor/approvals";
    }
}