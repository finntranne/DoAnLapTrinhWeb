package com.alotra.controller.shipper;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.security.MyUserDetails;
import com.alotra.service.order.ShipperOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/shipper/orders")
@PreAuthorize("hasAuthority('SHIPPER')")
@RequiredArgsConstructor
@Slf4j
public class ShipperOrderController {

    private final ShipperOrderService shipperOrderService;

    private Integer getUserIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userDetails.getUser().getId();
    }

    @GetMapping
    public String listOrders(@RequestParam(required = false) String status,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Integer shipperId = getUserIdOrThrow(userDetails);
            var orders = shipperOrderService.getShipperOrders(shipperId, status, searchQuery,
                    org.springframework.data.domain.PageRequest.of(page, 10));

            model.addAttribute("orders", orders.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", orders.getTotalPages());
            model.addAttribute("totalItems", orders.getTotalElements());
            model.addAttribute("status", status);
            model.addAttribute("searchQuery", searchQuery);
            model.addAttribute("assignedCount", shipperOrderService.countOrdersByStatus(shipperId, "Confirmed"));
            model.addAttribute("deliveringCount", shipperOrderService.countOrdersByStatus(shipperId, "Delivering"));
            model.addAttribute("completedCount", shipperOrderService.countOrdersByStatus(shipperId, "Completed"));
            model.addAttribute("currentUri", "/shipper/orders");

            return "shipper/orders/list";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "Ban can dang nhap voi tai khoan shipper");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Error loading shipper orders", e);
            redirectAttributes.addFlashAttribute("error", "Co loi xay ra khi tai danh sach don hang shipper.");
            return "redirect:/shipper/dashboard";
        }
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Integer id,
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Integer shipperId = getUserIdOrThrow(userDetails);
            var orderOpt = shipperOrderService.getOrderDetail(id, shipperId);
            if (orderOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Khong tim thay don hang hoac ban khong duoc phan cong.");
                return "redirect:/shipper/orders";
            }

            model.addAttribute("order", orderOpt.get());
            model.addAttribute("shippingHistory", shipperOrderService.getShippingHistory(id, shipperId));
            model.addAttribute("currentUri", "/shipper/orders");
            return "shipper/orders/detail";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "Ban can dang nhap voi tai khoan shipper");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Error loading shipper order detail {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Co loi xay ra khi tai chi tiet don hang.");
            return "redirect:/shipper/orders";
        }
    }

    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String imageURL,
            @AuthenticationPrincipal MyUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Integer shipperId = getUserIdOrThrow(userDetails);
            shipperOrderService.updateShippingStatus(id, shipperId, status, notes, imageURL);
            redirectAttributes.addFlashAttribute("success", buildSuccessMessage(status));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "Ban can dang nhap voi tai khoan shipper");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Error updating shipping status for order {}", id, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shipper/orders/" + id;
    }

    private String buildSuccessMessage(String status) {
        return switch (status) {
            case "Picking_Up" -> "Da cap nhat trang thai dang lay hang.";
            case "Delivering" -> "Da cap nhat trang thai dang giao hang.";
            case "Delivered" -> "Da xac nhan giao hang thanh cong.";
            case "Failed_Delivery" -> "Da bao giao hang that bai va tra don ve vendor.";
            default -> "Da cap nhat trang thai don hang.";
        };
    }
}
