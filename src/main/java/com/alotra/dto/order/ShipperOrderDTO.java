package com.alotra.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipperOrderDTO {
	private Integer orderId;
	private LocalDateTime orderDate;
	private String customerName;
	private String customerPhone;
	private String recipientName;
	private String recipientPhone;
	private String shippingAddress;
	private BigDecimal grandTotal;
	private String paymentMethod;
	private String paymentStatus;
	private String orderStatus;
	private String currentShippingStatus; // Trạng thái giao hàng hiện tại
	private Integer totalItems;
	private String notes;
	private String shopName;
	private String shopPhone;
	private String shopAddress;
	private LocalDateTime assignedAt; // Thời gian được giao đơn
	private LocalDateTime lastUpdateTime; // Cập nhật lần cuối
}