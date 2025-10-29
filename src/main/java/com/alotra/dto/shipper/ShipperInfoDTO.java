package com.alotra.dto.shipper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho thông tin shipper
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipperInfoDTO {
	private Integer userId;
	private String fullName;
	private String email;
	private String phoneNumber;
	private String avatarURL;
	private String shopName;
	private String shopAddress;
	private String shopPhone;
}