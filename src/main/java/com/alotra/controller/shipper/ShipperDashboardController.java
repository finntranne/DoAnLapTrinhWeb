package com.alotra.controller.shipper;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.security.MyUserDetails;
import com.alotra.service.order.ShipperOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/shipper")
@PreAuthorize("hasAuthority('SHIPPER')")
@RequiredArgsConstructor
@Slf4j
public class ShipperDashboardController {

    private final ShipperOrderService shipperOrderService;

    private Integer getUserIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userDetails.getUser().getId();
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal MyUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Integer shipperId = getUserIdOrThrow(userDetails);

            model.addAttribute("shipperInfo", shipperOrderService.getShipperInfo(shipperId));
            model.addAttribute("stats", shipperOrderService.getDashboardStats(shipperId));
            model.addAttribute("pendingOrders", shipperOrderService.getPendingOrders(shipperId, 5));
            model.addAttribute("deliveringOrders", shipperOrderService.getDeliveringOrders(shipperId, 5));
            model.addAttribute("currentUri", "/shipper/dashboard");

            return "shipper/dashboard";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "Ban can dang nhap voi tai khoan shipper");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Error loading shipper dashboard", e);
            redirectAttributes.addFlashAttribute("error", "Co loi xay ra khi tai dashboard shipper.");
            return "redirect:/";
        }
    }
}
