package com.alotra.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.alotra.entity.user.Notification;
import com.alotra.security.MyUserDetails;
import com.alotra.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@ControllerAdvice(assignableTypes = {
        HomeController.class,
        ProductController.class,
        CartController.class,
        OrderController.class,
        CustomerOrderController.class,
        CustomerAddressController.class,
        UserProfileController.class,
        ShopController.class,
        FavoriteController.class,
        ReviewController.class,
        ChatController.class
})
@RequiredArgsConstructor
public class GlobalCustomerControllerAdvice {

    private final NotificationService notificationService;

    private Integer getUserIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userDetails.getUser().getId();
    }

    @ModelAttribute("unreadNotifications")
    public List<Notification> getUnreadNotifications(@AuthenticationPrincipal MyUserDetails userDetails) {
        try {
            Integer userId = getUserIdOrThrow(userDetails);
            return notificationService.getUnreadNotificationsForUser(userId);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount(@AuthenticationPrincipal MyUserDetails userDetails) {
        try {
            Integer userId = getUserIdOrThrow(userDetails);
            return notificationService.countUnreadNotificationsForUser(userId);
        } catch (Exception e) {
            return 0L;
        }
    }
}
