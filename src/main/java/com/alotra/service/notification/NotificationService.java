package com.alotra.service.notification;

import com.alotra.entity.common.DeviceToken;
import com.alotra.entity.user.Notification;
import com.alotra.entity.user.User;
import com.alotra.repository.common.DeviceTokenRepository;
import com.alotra.repository.user.NotificationRepository;
import com.alotra.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;

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
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
        log.info("Notification created for user {}: {}", userId, title);

        sendPushNotification(userId, title, message, type, relatedEntityId);
    }

    public void notifyAdminsAboutNewApproval(String entityType, Integer entityId) {
        List<User> admins = userRepository.findByRoles_RoleName("ADMIN");

        String title = "Yêu cầu phê duyệt mới";
        String message = String.format(
                "Có yêu cầu phê duyệt %s mới (ID: %d) cần xem xét",
                entityType,
                entityId);

        admins.forEach(admin ->
                createNotification(admin.getId(), title, message, "Approval", entityId));

        log.info("Notified {} admins about new {} approval", admins.size(), entityType);
    }

    public void notifyCustomerAboutOrderStatus(Integer userId, Integer orderId, String newStatus) {
        String title = "C\u1eadp nh\u1eadt \u0111\u01a1n h\u00e0ng #" + orderId;
        String message = getOrderStatusMessage(newStatus);

        createNotification(userId, title, message, "OrderStatus", orderId);
    }

    public void notifyCustomerAboutOrderAssigned(Integer userId, Integer orderId) {
        String title = "C\u1eadp nh\u1eadt \u0111\u01a1n h\u00e0ng #" + orderId;
        String message = "\u0110\u01a1n h\u00e0ng c\u1ee7a b\u1ea1n \u0111\u00e3 \u0111\u01b0\u1ee3c giao cho shipper v\u00e0 \u0111ang ch\u1edd l\u1ea5y h\u00e0ng";

        createNotification(userId, title, message, "OrderStatus", orderId);
    }

    public void notifyVendorAboutNewOrder(Integer vendorUserId, Integer orderId, String customerName) {
        String title = "Đơn hàng mới #" + orderId;
        String message = String.format("Bạn có đơn hàng mới từ khách hàng %s", customerName);

        createNotification(vendorUserId, title, message, "NewOrder", orderId);
    }

    public void notifyAboutApprovalStatus(Integer userId, String entityType, Integer entityId,
                                          String status, String note) {
        String title = status.equals("Approved")
                ? "Yêu cầu đã được phê duyệt"
                : "Yêu cầu bị từ chối";

        String message = String.format(
                "Yêu cầu %s (ID: %d) đã %s. %s",
                entityType,
                entityId,
                status.equals("Approved") ? "được phê duyệt" : "bị từ chối",
                note != null ? "Ghi chú: " + note : "");

        createNotification(userId, title, message, "ApprovalResult", entityId);
    }

    public void notifyCustomersAboutPromotion(Integer promotionId, String promotionName, String promoCode) {
        List<User> customers = userRepository.findByRoles_RoleName("CUSTOMER");

        String title = "Khuyến mãi mới: " + promotionName;
        String message = String.format("Sử dụng mã %s để nhận ưu đãi đặc biệt!", promoCode);

        customers.forEach(customer ->
                createNotification(customer.getId(), title, message, "NewPromotion", promotionId));

        log.info("Notified {} customers about new promotion", customers.size());
    }

    public void notifyVendorAboutNewReview(Integer vendorUserId, Integer productId,
                                           String productName, Integer rating) {
        String title = "Đánh giá sản phẩm mới";
        String message = String.format("Sản phẩm '%s' nhận được đánh giá %d sao", productName, rating);

        createNotification(vendorUserId, title, message, "NewReview", productId);
    }

    public void notifyShipperAboutAssignment(Integer shipperId, Integer orderId, String deliveryAddress) {
        String title = "Đơn hàng được giao #" + orderId;
        String message = String.format("Bạn có đơn hàng mới cần giao đến: %s", deliveryAddress);

        createNotification(shipperId, title, message, "OrderAssignment", orderId);
    }

    public void sendSystemNotification(String title, String message) {
        List<User> allUsers = userRepository.findAll();

        allUsers.forEach(user ->
                createNotification(user.getId(), title, message, "System", null));

        log.info("System notification sent to {} users", allUsers.size());
    }

    private void sendPushNotification(Integer userId, String title, String message,
                                      String type, Integer relatedEntityId) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUser_IdAndIsActiveTrue(userId);

        if (tokens.isEmpty()) {
            log.debug("No active device tokens found for user {}", userId);
            return;
        }

        tokens.forEach(deviceToken -> {
            try {
                log.info("Push notification sent to device: {}", deviceToken.getDeviceType());
            } catch (Exception e) {
                log.error("Failed to send push notification to device", e);
                deviceToken.setIsActive(false);
                deviceTokenRepository.save(deviceToken);
            }
        });
    }

    private String getOrderStatusMessage(String status) {
        return switch (status) {
            case "Confirmed" -> "\u0110\u01a1n h\u00e0ng c\u1ee7a b\u1ea1n \u0111\u00e3 \u0111\u01b0\u1ee3c x\u00e1c nh\u1eadn v\u00e0 \u0111ang \u0111\u01b0\u1ee3c chu\u1ea9n b\u1ecb";
            case "Assigned" -> "\u0110\u01a1n h\u00e0ng c\u1ee7a b\u1ea1n \u0111\u00e3 \u0111\u01b0\u1ee3c giao cho shipper v\u00e0 \u0111ang ch\u1edd l\u1ea5y h\u00e0ng";
            case "Picking_Up" -> "Shipper \u0111ang \u0111\u1ebfn c\u1eeda h\u00e0ng \u0111\u1ec3 l\u1ea5y \u0111\u01a1n h\u00e0ng c\u1ee7a b\u1ea1n";
            case "Delivering" -> "\u0110\u01a1n h\u00e0ng c\u1ee7a b\u1ea1n \u0111ang tr\u00ean \u0111\u01b0\u1eddng giao \u0111\u1ebfn";
            case "Delivery_Attempt" -> "Shipper \u0111ang c\u1ed1 g\u1eafng li\u00ean h\u1ec7 \u0111\u1ec3 giao \u0111\u01a1n h\u00e0ng cho b\u1ea1n";
            case "Completed" -> "\u0110\u01a1n h\u00e0ng \u0111\u00e3 \u0111\u01b0\u1ee3c giao th\u00e0nh c\u00f4ng. C\u1ea3m \u01a1n b\u1ea1n \u0111\u00e3 mua h\u00e0ng!";
            case "Cancelled" -> "\u0110\u01a1n h\u00e0ng c\u1ee7a b\u1ea1n \u0111\u00e3 b\u1ecb h\u1ee7y";
            case "Returned" -> "\u0110\u01a1n h\u00e0ng c\u1ee7a b\u1ea1n \u0111\u00e3 \u0111\u01b0\u1ee3c tr\u1ea3 l\u1ea1i";
            case "Refunded" -> "\u0110\u01a1n h\u00e0ng c\u1ee7a b\u1ea1n \u0111\u00e3 \u0111\u01b0\u1ee3c ho\u00e0n ti\u1ec1n";
            default -> "Tr\u1ea1ng th\u00e1i \u0111\u01a1n h\u00e0ng: " + status;
        };
    }

    public List<Notification> getUnreadNotificationsForUser(Integer userId) {
        Pageable topFiveNewest = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        return notificationRepository.findUnreadNotificationsByUserId(userId, topFiveNewest);
    }

    public long countUnreadNotificationsForUser(Integer userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    private String generateLinkForNotification(String type) {
        String typeLower = type.toLowerCase();

        if (typeLower.contains("PRODUCT")) {
            return "/admin/products/pending";
        }
        if (typeLower.contains("TOPPING")) {
            return "/admin/toppings/pending";
        }
        if (typeLower.contains("PROMOTION")) {
            return "/admin/promotions/pending";
        }
        if (typeLower.contains("SHOP")) {
            return "/admin/shops/pending";
        }

        return "/admin/notifications/all";
    }
}
