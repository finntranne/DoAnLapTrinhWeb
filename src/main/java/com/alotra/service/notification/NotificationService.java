package com.alotra.service.notification;

import com.alotra.entity.common.DeviceToken;
import com.alotra.entity.user.Notification;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.repository.common.DeviceTokenRepository;
import com.alotra.repository.user.NotificationRepository;
import com.alotra.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    // private final FirebaseMessagingService firebaseMessaging; // Optional for push notifications

    // ============================================
    // NOTIFICATION CREATION
    // ============================================

    /**
     * Create notification for specific user
     */
    public void createNotification(Integer userId, String title, String message, 
                                   String type, Integer relatedEntityId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRelatedEntityID(relatedEntityId);
        notification.setIsRead(false);

        notificationRepository.save(notification);
        log.info("Notification created for user {}: {}", userId, title);

        // Send push notification if available
        sendPushNotification(userId, title, message, type, relatedEntityId);
    }

//    /**
//     * Create notification for multiple users
//     */
//    public void createBulkNotifications(List<Integer> userIds, String title, 
//                                       String message, String type, Integer relatedEntityId) {
//        List<User> users = userRepository.findAllById(userIds);
//
//        List<Notification> notifications = users.stream()
//            .map(user -> {
//                Notification notification = new Notification();
//                notification.setUser(user);
//                notification.setTitle(title);
//                notification.setMessage(message);
//                notification.setType(type);
//                notification.setRelatedEntityID(relatedEntityId);
//                notification.setIsRead(false);
//                return notification;
//            })
//            .collect(Collectors.toList());
//
//        notificationRepository.saveAll(notifications);
//        log.info("Bulk notifications created for {} users", userIds.size());
//
//        // Send push notifications
//        userIds.forEach(userId -> 
//            sendPushNotification(userId, title, message, type, relatedEntityId));
//    }

    // ============================================
    // SPECIFIC NOTIFICATION TYPES
    // ============================================

    /**
     * Notify admins about new approval request
     */
    public void notifyAdminsAboutNewApproval(String entityType, Integer entityId) {
        List<User> admins = userRepository.findByRoles_RoleName("ADMIN");

        String title = "Yêu cầu phê duyệt mới";
        String message = String.format("Có yêu cầu phê duyệt %s mới (ID: %d) cần xem xét", 
            entityType, entityId);

        admins.forEach(admin -> 
            createNotification(admin.getId(), title, message, "Approval", entityId));

        log.info("Notified {} admins about new {} approval", admins.size(), entityType);
    }

    /**
     * Notify customer about order status change
     */
    public void notifyCustomerAboutOrderStatus(Integer userId, Integer orderId, String newStatus) {
        String title = "Cập nhật đơn hàng #" + orderId;
        String message = getOrderStatusMessage(newStatus);

        createNotification(userId, title, message, "OrderStatus", orderId);
    }

    /**
     * Notify vendor about new order
     */
    public void notifyVendorAboutNewOrder(Integer vendorUserId, Integer orderId, String customerName) {
        String title = "Đơn hàng mới #" + orderId;
        String message = String.format("Bạn có đơn hàng mới từ khách hàng %s", customerName);

        createNotification(vendorUserId, title, message, "NewOrder", orderId);
    }

    /**
     * Notify about approval status
     */
    public void notifyAboutApprovalStatus(Integer userId, String entityType, Integer entityId, 
                                         String status, String note) {
        String title = status.equals("Approved") ? 
            "Yêu cầu đã được phê duyệt" : "Yêu cầu bị từ chối";
        
        String message = String.format("Yêu cầu %s (ID: %d) đã %s. %s", 
            entityType, entityId, 
            status.equals("Approved") ? "được phê duyệt" : "bị từ chối",
            note != null ? "Ghi chú: " + note : "");

        createNotification(userId, title, message, "ApprovalResult", entityId);
    }

    /**
     * Notify about new promotion
     */
    public void notifyCustomersAboutPromotion(Integer promotionId, String promotionName, 
                                              String promoCode) {
        List<User> customers = userRepository.findByRoles_RoleName("CUSTOMER");

        String title = "Khuyến mãi mới: " + promotionName;
        String message = String.format("Sử dụng mã %s để nhận ưu đãi đặc biệt!", promoCode);

        customers.forEach(customer -> 
            createNotification(customer.getId(), title, message, "NewPromotion", promotionId));

        log.info("Notified {} customers about new promotion", customers.size());
    }

    /**
     * Notify about product review
     */
    public void notifyVendorAboutNewReview(Integer vendorUserId, Integer productId, 
                                          String productName, Integer rating) {
        String title = "Đánh giá sản phẩm mới";
        String message = String.format("Sản phẩm '%s' nhận được đánh giá %d sao", 
            productName, rating);

        createNotification(vendorUserId, title, message, "NewReview", productId);
    }

    /**
     * Notify shipper about assigned order
     */
    public void notifyShipperAboutAssignment(Integer shipperId, Integer orderId, 
                                            String deliveryAddress) {
        String title = "Đơn hàng được giao #" + orderId;
        String message = String.format("Bạn có đơn hàng mới cần giao đến: %s", deliveryAddress);

        createNotification(shipperId, title, message, "OrderAssignment", orderId);
    }

    /**
     * System notification to all users
     */
    public void sendSystemNotification(String title, String message) {
        List<User> allUsers = userRepository.findAll();

        allUsers.forEach(user -> 
            createNotification(user.getId(), title, message, "System", null));

        log.info("System notification sent to {} users", allUsers.size());
    }

//    // ============================================
//    // NOTIFICATION RETRIEVAL
//    // ============================================
//
//    /**
//     * Get user notifications with pagination
//     */
//    public Page<NotificationDTO> getUserNotifications(Integer userId, Pageable pageable) {
//        Page<Notification> notifications = notificationRepository
//            .findByUser_UserIDOrderByCreatedAtDesc(userId, pageable);
//
//        return notifications.map(this::convertToDTO);
//    }
//
//    /**
//     * Get unread notifications
//     */
//    public List<NotificationDTO> getUnreadNotifications(Integer userId) {
//        List<Notification> notifications = notificationRepository
//            .findByUser_UserIDAndIsReadFalseOrderByCreatedAtDesc(userId);
//
//        return notifications.stream()
//            .map(this::convertToDTO)
//            .collect(Collectors.toList());
//    }
//
//    /**
//     * Get unread count
//     */
//    public Long getUnreadCount(Integer userId) {
//        return notificationRepository.countUnreadByUserId(userId);
//    }
//
//    /**
//     * Get notifications by type
//     */
//    public Page<NotificationDTO> getNotificationsByType(Integer userId, String type, 
//                                                       Pageable pageable) {
//        Page<Notification> notifications = notificationRepository
//            .findByUser_UserIDAndTypeOrderByCreatedAtDesc(userId, type, pageable);
//
//        return notifications.map(this::convertToDTO);
//    }
//
//    // ============================================
//    // NOTIFICATION ACTIONS
//    // ============================================
//
//    /**
//     * Mark notification as read
//     */
//    public void markAsRead(Integer userId, Integer notificationId) {
//        Notification notification = notificationRepository.findById(notificationId)
//            .orElseThrow(() -> new RuntimeException("Notification not found"));
//
//        if (!notification.getUser().getId().equals(userId)) {
//            throw new RuntimeException("Unauthorized access to notification");
//        }
//
//        notification.setIsRead(true);
//        notificationRepository.save(notification);
//    }
//
//    /**
//     * Mark multiple notifications as read
//     */
//    public void markMultipleAsRead(Integer userId, List<Integer> notificationIds) {
//        notificationRepository.markAsRead(userId, notificationIds);
//        log.info("Marked {} notifications as read for user {}", notificationIds.size(), userId);
//    }
//
//    /**
//     * Mark all notifications as read
//     */
//    public void markAllAsRead(Integer userId) {
//        notificationRepository.markAllAsRead(userId);
//        log.info("Marked all notifications as read for user {}", userId);
//    }
//
//    /**
//     * Delete notification
//     */
//    public void deleteNotification(Integer userId, Integer notificationId) {
//        Notification notification = notificationRepository.findById(notificationId)
//            .orElseThrow(() -> new RuntimeException("Notification not found"));
//
//        if (!notification.getUser().getId().equals(userId)) {
//            throw new RuntimeException("Unauthorized access to notification");
//        }
//
//        notificationRepository.delete(notification);
//        log.info("Notification {} deleted by user {}", notificationId, userId);
//    }
//
//    /**
//     * Delete old notifications (scheduled cleanup)
//     */
//    public void deleteOldNotifications(int daysOld) {
//        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
//        notificationRepository.deleteOldNotifications(cutoffDate);
//        log.info("Deleted notifications older than {} days", daysOld);
//    }
//
//    // ============================================
//    // DEVICE TOKEN MANAGEMENT
//    // ============================================
//
//    /**
//     * Register device token for push notifications
//     */
//    public void registerDeviceToken(Integer userId, String token, String deviceType) {
//        User user = userRepository.findById(userId)
//            .orElseThrow(() -> new RuntimeException("User not found"));
//
//        // Check if token already exists
//        deviceTokenRepository.findByDeviceToken(token).ifPresentOrElse(
//            existingToken -> {
//                existingToken.setIsActive(true);
//                existingToken.setUser(user);
//                deviceTokenRepository.save(existingToken);
//            },
//            () -> {
//                DeviceToken deviceToken = new DeviceToken();
//                deviceToken.setUser(user);
//                deviceToken.setDeviceToken(token);
//                deviceToken.setDeviceType(deviceType);
//                deviceToken.setIsActive(true);
//                deviceTokenRepository.save(deviceToken);
//            }
//        );
//
//        log.info("Device token registered for user {}", userId);
//    }
//
//    /**
//     * Unregister device token
//     */
//    public void unregisterDeviceToken(String token) {
//        deviceTokenRepository.deactivateToken(token);
//        log.info("Device token unregistered: {}", token);
//    }
//
//    /**
//     * Unregister all tokens for user (logout)
//     */
//    public void unregisterAllUserTokens(Integer userId) {
//        deviceTokenRepository.deactivateAllUserTokens(userId);
//        log.info("All device tokens unregistered for user {}", userId);
//    }
//
//    // ============================================
//    // PUSH NOTIFICATION (FIREBASE)
//    // ============================================
//
    /**
     * Send push notification to user's devices
     */
    private void sendPushNotification(Integer userId, String title, String message, 
                                     String type, Integer relatedEntityId) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUser_IdAndIsActiveTrue(userId);

        if (tokens.isEmpty()) {
            log.debug("No active device tokens found for user {}", userId);
            return;
        }

        // Send to each device
        tokens.forEach(deviceToken -> {
            try {
                // Firebase Cloud Messaging implementation
                // firebaseMessaging.sendNotification(deviceToken.getDeviceToken(), title, message, type, relatedEntityId);
                log.info("Push notification sent to device: {}", deviceToken.getDeviceType());
            } catch (Exception e) {
                log.error("Failed to send push notification to device", e);
                // Deactivate token if it's invalid
                deviceToken.setIsActive(false);
                deviceTokenRepository.save(deviceToken);
            }
        });
    }
//
//    // ============================================
//    // HELPER METHODS
//    // ============================================
//
//    private NotificationDTO convertToDTO(Notification notification) {
//        NotificationDTO dto = new NotificationDTO();
//        dto.setNotificationId(notification.getNotificationId());
//        dto.setTitle(notification.getTitle());
//        dto.setMessage(notification.getMessage());
//        dto.setType(notification.getType());
//        dto.setRelatedEntityId(notification.getRelatedEntityId());
//        dto.setIsRead(notification.getIsRead());
//        dto.setCreatedAt(notification.getCreatedAt());
//        return dto;
//    }
//
    private String getOrderStatusMessage(String status) {
        switch (status) {
            case "Confirmed":
                return "Đơn hàng của bạn đã được xác nhận và đang được chuẩn bị";
            case "Delivering":
                return "Đơn hàng của bạn đang trên đường giao đến";
            case "Completed":
                return "Đơn hàng đã được giao thành công. Cảm ơn bạn đã mua hàng!";
            case "Cancelled":
                return "Đơn hàng của bạn đã bị hủy";
            case "Returned":
                return "Đơn hàng của bạn đã được trả lại";
            case "Refunded":
                return "Đơn hàng của bạn đã được hoàn tiền";
            default:
                return "Trạng thái đơn hàng: " + status;
        }
    }
}