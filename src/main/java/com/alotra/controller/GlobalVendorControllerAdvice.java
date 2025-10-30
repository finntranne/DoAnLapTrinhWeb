package com.alotra.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.alotra.entity.common.MessageEntity;
import com.alotra.entity.user.Notification;
import com.alotra.security.MyUserDetails;
import com.alotra.service.chat.ChatService;
import com.alotra.service.notification.NotificationService;

@ControllerAdvice(basePackages = "com.alotra.controller.vendor")
public class GlobalVendorControllerAdvice {
	
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
	ChatService chatService;

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
	 * Lấy shopId từ authenticated user Throw exception nếu user chưa có shop
	 */
	private Integer getShopIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
		if (userDetails == null) {
			throw new IllegalStateException("User is not authenticated");
		}

		Integer shopId = userDetails.getShopId();

		if (shopId == null) {
			throw new IllegalStateException("Bạn chưa đăng ký shop. Vui lòng đăng ký shop trước.");
		}

		return shopId;
	}
    
    @ModelAttribute("recentMessages")
    public List<MessageEntity> getRecentMessages(@AuthenticationPrincipal MyUserDetails userDetails){
    	try {
            Integer shopId = getShopIdOrThrow(userDetails);
            return chatService.findRecentMessagesForShop(shopId, 5);        
            } catch (Exception e) {
            return Collections.emptyList(); 
        }
    }
    
    @ModelAttribute("unreadCount")
    public long getUnreadMessageCount(@AuthenticationPrincipal MyUserDetails userDetails) {
        try {
        	Integer shopId = getShopIdOrThrow(userDetails);
            return chatService.countUnreadMessages(shopId);
        } catch (Exception e) {
            return 0L;
        }
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