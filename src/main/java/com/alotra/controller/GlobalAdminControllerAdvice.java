package com.alotra.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.alotra.entity.user.Notification;
import com.alotra.entity.user.User;
import com.alotra.service.notification.NotificationService;
import com.alotra.service.user.IUserService;

@ControllerAdvice(basePackages = "com.alotra.controller.admin")
public class GlobalAdminControllerAdvice {
	
	// 1. Chỉ cần NotificationService
    @Autowired
    private NotificationService notificationService;

    // ID người dùng mặc định
    private static final Integer DEFAULT_USER_ID = 1;

    /**
     * Tự động thêm danh sách 5 thông báo chưa đọc của User ID = 1 vào Model.
     */
    @ModelAttribute("unreadNotifications")
    public List<Notification> getUnreadNotifications() {
        try {
            // 2. Gọi thẳng service với ID = 1
            return notificationService.getUnreadNotificationsForUser(DEFAULT_USER_ID);
        } catch (Exception e) {
            // Thêm try-catch để an toàn, nếu User ID 1 không tồn tại
            return Collections.emptyList(); 
        }
    }

    /**
     * Tự động thêm TỔNG số thông báo chưa đọc của User ID = 1 vào Model.
     */
    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount() {
        try {
            // 3. Gọi thẳng service với ID = 1
            return notificationService.countUnreadNotificationsForUser(DEFAULT_USER_ID);
        } catch (Exception e) {
            return 0L;
        }
    }
}
