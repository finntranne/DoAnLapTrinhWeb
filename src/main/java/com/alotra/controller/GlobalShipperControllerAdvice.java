package com.alotra.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.alotra.entity.user.Notification;
import com.alotra.security.MyUserDetails;
import com.alotra.service.notification.NotificationService;

@ControllerAdvice(basePackages = "com.alotra.controller.shipper")
public class GlobalShipperControllerAdvice {
	
    @Autowired
    private NotificationService notificationService;

    /**
     * Lấy userId từ authenticated user
     */
    private Integer getUserIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userDetails.getUser().getId();
    }
    
    /**
     * Tự động thêm danh sách 5 thông báo chưa đọc vào Model
     */
    @ModelAttribute("unreadNotifications")
    public List<Notification> getUnreadNotifications(@AuthenticationPrincipal MyUserDetails userDetails) {
        try {
            Integer userId = getUserIdOrThrow(userDetails);
            return notificationService.getUnreadNotificationsForUser(userId);
        } catch (Exception e) {
            return Collections.emptyList(); 
        }
    }

    /**
     * Tự động thêm TỔNG số thông báo chưa đọc vào Model
     */
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