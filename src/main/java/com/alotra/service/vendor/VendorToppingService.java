package com.alotra.service.vendor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.topping.ToppingRequestDTO;
import com.alotra.dto.topping.ToppingStatisticsDTO;
import com.alotra.entity.product.Topping;
import com.alotra.entity.product.ToppingApproval;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.repository.product.ToppingApprovalRepository;
import com.alotra.repository.product.ToppingRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.user.UserRepository;
import com.alotra.service.cloudinary.CloudinaryService;
import com.alotra.service.notification.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorToppingService {

	private final ShopRepository shopRepository;
	private final CloudinaryService cloudinaryService;
	private final NotificationService notificationService;
	private final UserRepository userRepository;
	private final ToppingRepository toppingRepository;
	private final ToppingApprovalRepository toppingApprovalRepository;

	private final ObjectMapper objectMapper;
	@PersistenceContext // Inject EntityManager for JPQL
	private EntityManager entityManager;

	// ==================== TOPPING MANAGEMENT ====================

	public Page<ToppingStatisticsDTO> getShopToppings(Integer shopId, Byte status, String search, Pageable pageable) {
		Page<Topping> toppings = toppingRepository.findShopToppingsFiltered(shopId, status, search, pageable);

		return toppings.map(topping -> {
			String approvalStatus = null;
			String activityStatus;

			Optional<ToppingApproval> latestApprovalOpt = toppingApprovalRepository
					.findTopByTopping_ToppingIDOrderByRequestedAtDesc(topping.getToppingID());

			if (latestApprovalOpt.isPresent()) {
				ToppingApproval latestApproval = latestApprovalOpt.get();
				String currentDbStatus = latestApproval.getStatus();

				if ("Pending".equals(currentDbStatus) || "Rejected".equals(currentDbStatus)) {
					String actionTypeText = "";
					switch (latestApproval.getActionType()) {
					case "CREATE":
						actionTypeText = "Tạo mới";
						break;
					case "UPDATE":
						actionTypeText = "Cập nhật";
						break;
					case "DELETE":
						actionTypeText = "Xóa";
						break;
					default:
						actionTypeText = latestApproval.getActionType();
					}
					approvalStatus = ("Pending".equals(currentDbStatus) ? "Đang chờ: " : "Bị từ chối: ")
							+ actionTypeText;
				}
			}

			activityStatus = (topping.getStatus() == 1) ? "Đang hoạt động" : "Không hoạt động";

			return new ToppingStatisticsDTO(topping, approvalStatus, activityStatus);
		});
	}

	public void requestToppingCreation(Integer shopId, ToppingRequestDTO request, Integer userId) throws Exception { // Ném
																														// Exception
		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		String uploadedImageUrl = null;
		// *** THÊM LOGIC UPLOAD ẢNH ***
		if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
			try {
				// Upload lên Cloudinary vào thư mục "toppings"
				Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(request.getImageFile(),
						"toppings", userId);
				uploadedImageUrl = uploadResult.get("secure_url");
			} catch (Exception e) {
				log.error("Lỗi upload ảnh topping: {}", e.getMessage(), e);
				throw new RuntimeException("Lỗi khi upload hình ảnh topping: " + e.getMessage());
			}
		}
		// *** KẾT THÚC LOGIC UPLOAD ẢNH ***

		// Tạo Topping mới với Status = 0 (Inactive)
		Topping topping = new Topping();
		topping.setShop(shop);
		topping.setToppingName(request.getToppingName());
		topping.setAdditionalPrice(request.getAdditionalPrice());
		topping.setImageURL(uploadedImageUrl); // *** SỬA: Gán URL đã upload ***
		topping.setStatus((byte) 0); // Chờ duyệt

		topping = toppingRepository.save(topping); // Lưu topping để lấy ID

		// Tạo yêu cầu phê duyệt
		ToppingApproval approval = new ToppingApproval();
		approval.setTopping(topping);
		approval.setShop(shop);
		approval.setActionType("CREATE");
		approval.setStatus("Pending");

		request.setImageURL(uploadedImageUrl); // *** THÊM: Gán URL vào DTO trước khi lưu JSON ***
		approval.setChangeDetails(objectMapper.writeValueAsString(request));

		approval.setRequestedBy(user);

		toppingApprovalRepository.save(approval);

		notificationService.notifyAdminsAboutNewApproval("TOPPING", topping.getToppingID());
	}

	public Topping getToppingDetail(Integer shopId, Integer toppingId) {
		Topping topping = toppingRepository.findById(toppingId)
				.orElseThrow(() -> new RuntimeException("Topping not found"));
		if (!topping.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Topping does not belong to this shop");
		}
		return topping;
	}

	public ToppingRequestDTO convertToppingToDTO(Topping topping) {
		ToppingRequestDTO dto = new ToppingRequestDTO();
		dto.setToppingId(topping.getToppingID());
		dto.setToppingName(topping.getToppingName());
		dto.setAdditionalPrice(topping.getAdditionalPrice());
		dto.setImageURL(topping.getImageURL());
		return dto;
	}

	public void requestToppingUpdate(Integer shopId, ToppingRequestDTO request, Integer userId) throws Exception { // Ném
																													// Exception
		Topping topping = getToppingDetail(shopId, request.getToppingId()); // Kiểm tra quyền sở hữu
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		List<ToppingApproval> existingApprovals = toppingApprovalRepository
				.findByTopping_ToppingIDAndStatus(topping.getToppingID(), "Pending");
		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho topping này");
		}

		// *** THÊM LOGIC UPLOAD ẢNH (CHO UPDATE) ***
		if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
			// Nếu có file mới, upload và set URL mới cho DTO
			try {
				Map<String, String> uploadResult = cloudinaryService.uploadImageAndReturnDetails(request.getImageFile(),
						"toppings", userId);
				request.setImageURL(uploadResult.get("secure_url")); // Gán URL mới vào DTO
			} catch (Exception e) {
				log.error("Lỗi upload ảnh topping (update): {}", e.getMessage(), e);
				throw new RuntimeException("Lỗi khi upload hình ảnh topping: " + e.getMessage());
			}
		} else {
			// Nếu không có file mới, giữ lại URL ảnh cũ từ database
			request.setImageURL(topping.getImageURL());
		}
		// *** KẾT THÚC LOGIC UPLOAD ẢNH ***

		ToppingApproval approval = new ToppingApproval();
		approval.setTopping(topping);
		approval.setShop(topping.getShop());
		approval.setActionType("UPDATE");
		approval.setStatus("Pending");
		approval.setChangeDetails(objectMapper.writeValueAsString(request)); // Lưu thay đổi (đã bao gồm imageURL)
		approval.setRequestedBy(user);

		toppingApprovalRepository.save(approval);
		notificationService.notifyAdminsAboutNewApproval("TOPPING", topping.getToppingID());
	}

	public void requestToppingDeletion(Integer shopId, Integer toppingId, Integer userId) {
		Topping topping = getToppingDetail(shopId, toppingId); // Kiểm tra quyền sở hữu
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		List<ToppingApproval> existingApprovals = toppingApprovalRepository
				.findByTopping_ToppingIDAndStatus(topping.getToppingID(), "Pending");
		if (!existingApprovals.isEmpty()) {
			throw new RuntimeException("Đã có yêu cầu đang chờ phê duyệt cho topping này");
		}

		ToppingApproval approval = new ToppingApproval();
		approval.setTopping(topping);
		approval.setShop(topping.getShop());
		approval.setActionType("DELETE");
		approval.setStatus("Pending");
		approval.setRequestedBy(user);

		toppingApprovalRepository.save(approval);
		// (Tùy chọn) Gửi thông báo
	}

}
