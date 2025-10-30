package com.alotra.controller;

import com.alotra.config.VNPayConfig;
import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.location.Address;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderDetail;
import com.alotra.entity.order.OrderDetailTopping;
import com.alotra.entity.order.OrderDetailToppingId;
import com.alotra.entity.order.OrderHistory;
import com.alotra.entity.shop.ShopEmployee;
import com.alotra.entity.promotion.Promotion; // ✅ THÊM IMPORT
import com.alotra.entity.user.User;
import com.alotra.model.ProductSaleDTO;
import com.alotra.repository.cart.CartItemRepository;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.order.OrderDetailRepository;
import com.alotra.repository.order.OrderDetailToppingRepository;
import com.alotra.repository.order.OrderHistoryRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.shop.ShopEmployeeRepository;
import com.alotra.repository.location.AddressRepository;
import com.alotra.repository.promotion.PromotionRepository; // ✅ THÊM REPOSITORY
import com.alotra.service.cart.CartService;
import com.alotra.service.order.ShipperOrderService;
import com.alotra.service.checkout.VNPayService;
import com.alotra.service.notification.NotificationService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.product.ProductService;
import com.alotra.service.shop.StoreService;
import com.alotra.service.user.UserService;
import com.alotra.util.VNPayUtil;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j; // ✅ THÊM SLF4J

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@Slf4j // ✅ SỬ DỤNG LOGGER
public class OrderController {

	// Hằng số cho VietQR (Ví dụ về một ngân hàng và tài khoản)
	private static final String VIETQR_BANK_ID = "970405";
	private static final String VIETQR_ACCOUNT_NO = "4303205336551";
	private static final String VIETQR_API_URL = "https://img.vietqr.io/image/";
	private static final String VIETQR_TEMPLATE = "compact";
	private static final String VIETQR_ADD_INFO = "Thanh toan don hang #";
	private static final String VIETQR_ACCOUNT_NAME = "TRAN HUU THOAI";

	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private CartService cartService;
	@Autowired
	private CartRepository cartRepository;
	@Autowired
	private CartItemRepository cartItemRepository;
	@Autowired
	private AddressRepository addressRepository;
	@Autowired
	private CategoryService categoryService;
	@Autowired
	private OrderDetailRepository orderDetailRepository;
	@Autowired
	private OrderDetailToppingRepository orderDetailToppingRepository;
	@Autowired
	private UserService userService;
	@Autowired
	private VNPayService vnPayService;
	@Autowired
	private StoreService storeService;
	@Autowired
	private ProductService productService;
	@Autowired
	private PromotionRepository promotionRepository; // ✅ INJECT PROMOTION REPOSITORY
	@Autowired
	private OrderHistoryRepository orderHistoryRepository;
	@Autowired
	private ShipperOrderService shipperOrderService;
	@Autowired
	private ShopEmployeeRepository shopEmployeeRepository;
	@Autowired
	private com.alotra.config.VNPayConfig vnPayConfig;
	@Autowired
	private NotificationService notificationService;

	// === HÀM TRỢ GIÚP: Lấy User (Giữ nguyên) ===
	private User getCurrentAuthenticatedUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
		}
		String username = auth.getName();
		return userService.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + username));
	}

	// --- Hàm trợ giúp lấy số lượng giỏ hàng (Giữ nguyên) ---
	private int getCurrentCartItemCount() {
		try {
			User user = getCurrentAuthenticatedUser();
			return cartService.getCartItemCount(user);
		} catch (ResponseStatusException | UsernameNotFoundException e) {
			return 0;
		}
	}

	// --- Helper Lấy Shop ID từ Session ---
	private Integer getSelectedShopId(HttpSession session) {
		Integer selectedShopId = (Integer) session.getAttribute("selectedShopId");
		return (selectedShopId == null) ? 0 : selectedShopId;
	}

	private User assignShipperAutomatically(Integer shopId) {
		// Lấy danh sách shipper active của shop
		List<ShopEmployee> activeEmployees = shopEmployeeRepository.findByShop_ShopIdAndStatus(shopId, "Active");

		List<User> shippers = activeEmployees.stream().map(ShopEmployee::getUser)
				.filter(user -> user.getRoles().stream().anyMatch(role -> "SHIPPER".equals(role.getRoleName())))
				.collect(Collectors.toList());

		if (shippers.isEmpty()) {
			return null;
		}

		// Tìm shipper có ít đơn đang xử lý nhất
		User selectedShipper = null;
		long minActiveOrders = Long.MAX_VALUE;

		for (User shipper : shippers) {
			long activeOrderCount = orderRepository.countByShipper_IdAndOrderStatus(shipper.getId(), "Delivering")
					+ orderRepository.countByShipper_IdAndOrderStatus(shipper.getId(), "Confirmed");

			if (activeOrderCount < minActiveOrders) {
				minActiveOrders = activeOrderCount;
				selectedShipper = shipper;
			}
		}

		return selectedShipper;
	}

	// --- Hàm CHỌN ITEM CHECKOUT (Giữ nguyên) ---
	@PostMapping("/cart/select-for-checkout")
	public String selectItemsForCheckout(
			@RequestParam(name = "selectedItemIds", required = false) List<Integer> selectedItemIds,
			HttpSession session, RedirectAttributes redirectAttributes) {

		if (selectedItemIds == null || selectedItemIds.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một sản phẩm để thanh toán.");
			return "redirect:/cart";
		}

		try {
			User user = getCurrentAuthenticatedUser();
			Cart cart = cartRepository.findByUser_Id(user.getId())
					.orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

			Set<Integer> userCartItemIds = cart.getItems().stream().map(CartItem::getCartItemID)
					.collect(Collectors.toSet());

			List<Integer> validSelectedIds = selectedItemIds.stream().filter(userCartItemIds::contains)
					.collect(Collectors.toList());

			if (validSelectedIds.isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMessage",
						"Sản phẩm đã chọn không hợp lệ hoặc không còn trong giỏ.");
				return "redirect:/cart";
			}

			session.setAttribute("selectedCheckoutItemIds", validSelectedIds);
			return "redirect:/checkout";

		} catch (ResponseStatusException | UsernameNotFoundException e) {
			return "redirect:/login";
		} catch (Exception e) {
			log.error("Lỗi chọn item checkout: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi chọn sản phẩm.");
			return "redirect:/cart";
		}
	}

	/**
	 * *** HIỂN THỊ TRANG CHECKOUT ***
	 */
	@GetMapping("/checkout")
	public String showCheckoutPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {

		try {
			Integer selectedShopId = getSelectedShopId(session);

			@SuppressWarnings("unchecked")
			List<Integer> selectedItemIds = (List<Integer>) session.getAttribute("selectedCheckoutItemIds");

			if (selectedItemIds == null || selectedItemIds.isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn sản phẩm từ giỏ hàng trước.");
				return "redirect:/cart";
			}

			User user = getCurrentAuthenticatedUser();
			Cart cart = cartRepository.findByUser_Id(user.getId())
					.orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

			// Lọc items
			List<CartItem> itemsToCheckout = cart.getItems().stream()
					.filter(item -> selectedItemIds.contains(item.getCartItemID())).collect(Collectors.toList());

			if (itemsToCheckout.isEmpty()) {
				session.removeAttribute("selectedCheckoutItemIds");
				redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm đã chọn không còn trong giỏ.");
				return "redirect:/cart";
			}

			// Tạo ViewModel
			List<Map<String, Object>> checkoutItemVMs = new ArrayList<>();
			for (CartItem item : itemsToCheckout) {
				Map<String, Object> vm = new HashMap<>();
				vm.put("item", item);
				vm.put("lineTotal", cartService.getLineTotal(item));
				checkoutItemVMs.add(vm);
			}

			// Tính toán tổng tiền TẠM TÍNH
			BigDecimal subtotal = cartService.calculateSubtotal(new HashSet<>(itemsToCheckout));

			BigDecimal shippingFee = new BigDecimal("20000");
			BigDecimal discount = BigDecimal.ZERO; // Khởi tạo ban đầu
			BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount); // Tính GrandTotal ban đầu

			// ✅ XỬ LÝ MÃ GIẢM GIÁ TỪ SESSION
			BigDecimal sessionDiscount = (BigDecimal) session.getAttribute("currentDiscountAmount");
			String couponCode = (String) session.getAttribute("currentCouponCode");

			if (sessionDiscount != null) {
				discount = sessionDiscount;
				// ✅ SỬA LỖI: CẬP NHẬT LẠI grandTotal sau khi có discount
				grandTotal = subtotal.add(shippingFee).subtract(discount);
				model.addAttribute("currentCouponCode", couponCode);
			}

			List<Address> addresses = addressRepository.findByUserId(user.getId());

			// Đưa thông tin ra View
			model.addAttribute("checkoutItemVMs", checkoutItemVMs);
			model.addAttribute("subtotal", subtotal);
			model.addAttribute("shippingFee", shippingFee);
			model.addAttribute("discount", discount);
			model.addAttribute("grandTotal", grandTotal);
			model.addAttribute("addresses", addresses);
			model.addAttribute("user", user);

			model.addAttribute("cartItemCount", getCurrentCartItemCount());
			model.addAttribute("categories", categoryService.findAll());
			model.addAttribute("shops", storeService.findAllActiveShops());
			model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

			return "shop/checkout";

		} catch (ResponseStatusException | UsernameNotFoundException e) {
			return "redirect:/login";
		} catch (Exception e) {
			log.error("Lỗi khi tải trang checkout: {}", e.getMessage(), e);
			session.removeAttribute("selectedCheckoutItemIds");
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tải trang thanh toán.");
			return "redirect:/cart";
		}
	}

	/**
	 * *** XỬ LÝ ÁP DỤNG MÃ GIẢM GIÁ (API MỚI) ***
	 */
	@PostMapping("/apply-coupon")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> applyCoupon(@RequestParam("couponCode") String couponCode,
			@RequestParam(name = "selectedItemIds", required = false) List<Integer> requestItemIds,
			HttpSession session) {

		Map<String, Object> response = new HashMap<>();

		try {
			User user = getCurrentAuthenticatedUser();

			Integer selectedShopId = getSelectedShopId(session);

			// 1. TÌM DANH SÁCH ID CUỐI CÙNG (Khắc phục lỗi effectively final)
			List<Integer> finalSelectedItemIds;

			if (requestItemIds != null && !requestItemIds.isEmpty()) {
				finalSelectedItemIds = requestItemIds; // Ưu tiên ID từ request
			} else {
				// Fallback về Session
				@SuppressWarnings("unchecked")
				List<Integer> sessionIds = (List<Integer>) session.getAttribute("selectedCheckoutItemIds");
				finalSelectedItemIds = sessionIds;
			}

			if (finalSelectedItemIds == null || finalSelectedItemIds.isEmpty()) {
				response.put("error", "Phiên làm việc hết hạn. Vui lòng làm mới trang hoặc quay lại giỏ hàng.");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			// 1. Tìm kiếm và kiểm tra mã giảm giá
			Optional<Promotion> promotionOpt = promotionRepository.findByPromoCodeAndStatusAndCreatedByShopID_ShopId(
					couponCode.toUpperCase(), (byte) 1, selectedShopId);
			Promotion promotion = promotionOpt.orElse(null);

			if (promotion == null || !"ORDER".equalsIgnoreCase(promotion.getPromotionType())) {
				response.put("error", "Mã giảm giá không tồn tại hoặc không áp dụng cho đơn hàng.");
				return ResponseEntity.badRequest().body(response);
			}

			// 2. Kiểm tra các điều kiện (Ngày hết hạn, số lượng, v.v.)
			if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(LocalDateTime.now())) {
				response.put("error", "Mã giảm giá đã hết hạn.");
				return ResponseEntity.badRequest().body(response);
			}

			// 3. Tải các Cart Item
			Cart cart = cartRepository.findByUser_Id(user.getId())
					.orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

			List<CartItem> itemsToCheckout = cart.getItems().stream()
					.filter(item -> finalSelectedItemIds.contains(item.getCartItemID())).collect(Collectors.toList());

			if (itemsToCheckout.isEmpty()) {
				response.put("error", "Sản phẩm đã chọn không còn trong giỏ.");
				return ResponseEntity.badRequest().body(response);
			}

			// 4. Tính toán giảm giá và tổng tiền mới
			BigDecimal subtotal = cartService.calculateSubtotal(new HashSet<>(itemsToCheckout));

			// Kiểm tra giá trị đơn hàng tối thiểu
			if (promotion.getMinOrderValue() != null && subtotal.compareTo(promotion.getMinOrderValue()) < 0) {
				response.put("error",
						"Đơn hàng chưa đạt giá trị tối thiểu (" + promotion.getMinOrderValue().toPlainString() + "đ).");
				return ResponseEntity.badRequest().body(response);
			}

			// Áp dụng giảm giá
			BigDecimal discountAmount = cartService.calculateDiscountAmount(subtotal, promotion);
			BigDecimal shippingFee = new BigDecimal("20000");
			BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discountAmount);

			// 5. Lưu mã giảm giá và số tiền giảm vào Session
			session.setAttribute("currentCouponCode", couponCode.toUpperCase());
			session.setAttribute("currentDiscountAmount", discountAmount);
			session.setAttribute("selectedCheckoutItemIds", finalSelectedItemIds);

			// 6. Trả về kết quả
			response.put("success", "Áp dụng mã giảm giá thành công!");
			response.put("discountAmount", discountAmount.toPlainString());
			response.put("grandTotal", grandTotal.toPlainString());
			response.put("couponCode", couponCode.toUpperCase());

			return ResponseEntity.ok(response);

		} catch (ResponseStatusException | UsernameNotFoundException e) {
			response.put("error", "Vui lòng đăng nhập.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		} catch (Exception e) {
			log.error("Error applying coupon", e);
			response.put("error", "Lỗi hệ thống: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	/**
	 * API xóa mã giảm giá khỏi session (Dùng cho nút 'Xóa') Endpoint:
	 * /remove-coupon
	 */
	@PostMapping("/remove-coupon")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> removeCoupon(HttpSession session) {
		session.removeAttribute("currentCouponCode");
		session.removeAttribute("currentDiscountAmount");

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		return ResponseEntity.ok(response);
	}

	/**
	 * *** SỬA LỚN: XỬ LÝ ĐẶT HÀNG (ĐẢM BẢO SUBTOTAL VÀ ID TRƯỚC KHI LƯU) ***
	 */
	@PostMapping("/place-order")
	@Transactional
	public Object placeOrder(@RequestParam(required = false) Integer addressId,
			@RequestParam(required = false) String notes, @RequestParam(required = false) String paymentMethod,
			@RequestParam(name = "selectedItemIds") List<Integer> selectedItemIds, HttpServletRequest request,
			HttpSession session, RedirectAttributes redirectAttributes) {

		if (addressId == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn địa chỉ giao hàng.");
			return "redirect:/checkout";
		}

		if (paymentMethod == null || paymentMethod.isBlank()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn phương thức thanh toán.");
			return "redirect:/checkout";
		}

		Order savedOrder = null;

		try {
			// 1-5. Lấy data và tính tổng (Giữ nguyên)
			if (selectedItemIds == null || selectedItemIds.isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMessage", "Không có sản phẩm nào được chọn để đặt hàng.");
				return "redirect:/cart";
			}

			User user = getCurrentAuthenticatedUser();
			Cart cart = cartRepository.findByUser_Id(user.getId())
					.orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giỏ hàng"));

			Map<Integer, CartItem> cartItemMap = cart.getItems().stream()
					.filter(item -> selectedItemIds.contains(item.getCartItemID()))
					.collect(Collectors.toMap(CartItem::getCartItemID, item -> item));

			List<CartItem> itemsToOrder = new ArrayList<>(cartItemMap.values());

			if (itemsToOrder.isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMessage", "Không có sản phẩm hợp lệ trong giỏ hàng.");
				return "redirect:/cart";
			}

			Address chosenAddress = addressRepository.findById(addressId)
					.orElseThrow(() -> new EntityNotFoundException("Địa chỉ giao hàng không hợp lệ."));

			if (!chosenAddress.getUser().getId().equals(user.getId())) {
				throw new AccessDeniedException("Bạn không có quyền dùng địa chỉ này.");
			}

			BigDecimal subtotal = cartService.calculateSubtotal(new HashSet<>(itemsToOrder));
			BigDecimal shippingFee = new BigDecimal("20000");
			BigDecimal discount = BigDecimal.ZERO;

			// ✅ ĐỌC MÃ GIẢM GIÁ TỪ SESSION
			BigDecimal sessionDiscount = (BigDecimal) session.getAttribute("currentDiscountAmount");
			if (sessionDiscount != null) {
				discount = sessionDiscount;
				// Xóa mã giảm giá khỏi session ngay sau khi lấy ra
				session.removeAttribute("currentCouponCode");
				session.removeAttribute("currentDiscountAmount");
			}

			// TÍNH GRAND TOTAL CUỐI CÙNG
			BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);

			// 6. Tạo và lưu Order (Giữ nguyên)
			Order order = new Order();
			order.setUser(user);
			order.setShop(itemsToOrder.get(0).getVariant().getProduct().getShop());
			order.setPaymentMethod(paymentMethod);
			order.setShippingAddress(chosenAddress.getFullAddress());
			order.setRecipientName(chosenAddress.getRecipientName());
			order.setRecipientPhone(chosenAddress.getPhoneNumber());
			order.setSubtotal(subtotal);
			order.setShippingFee(shippingFee);
			order.setDiscountAmount(discount); // ✅ LƯU SỐ TIỀN GIẢM VÀO DB
			order.setGrandTotal(grandTotal);
			order.setNotes(notes);

			// *** TỰ ĐỘNG GÁN SHIPPER ***
			User assignedShipper = assignShipperAutomatically(order.getShop().getShopId());
			if (assignedShipper != null) {
				order.setShipper(assignedShipper);
			}

			savedOrder = orderRepository.saveAndFlush(order);
			
			// Gửi thông báo
			try {
				notificationService.notifyVendorAboutNewOrder(
						itemsToOrder.get(0).getVariant().getProduct().getShop().getUser().getId(), order.getOrderID(),
						user.getFullName());
				notificationService.notifyShipperAboutAssignment(order.getShipper().getId(), order.getOrderID(), order.getShippingAddress());
			} catch (Exception e) {
				log.error("Error sending notification: {}", e.getMessage());
			}

			// === 7. SỬA LỚN: Xử lý OrderDetail và Topping (Giữ nguyên) ===
			List<OrderDetail> orderDetailList = new ArrayList<>();
			List<OrderDetailTopping> orderDetailToppingList = new ArrayList<>();

			Integer selectedShopId = getSelectedShopId(session);

			for (CartItem cartItem : itemsToOrder) {

				// --- 7a. TÍNH TOÁN GIÁ ĐƠN VỊ ĐÃ GIẢM ---
				BigDecimal baseVariantPrice = cartItem.getVariant().getPrice();
				Integer discountPercent = null;
				if (cartItem.getVariant().getProduct() != null
						&& cartItem.getVariant().getProduct().getProductID() != null) {
					Optional<ProductSaleDTO> saleDTOOpt = productService
							.findProductSaleDataById(cartItem.getVariant().getProduct().getProductID(), selectedShopId);
					if (saleDTOOpt.isPresent()) {
						discountPercent = saleDTOOpt.get().getDiscountPercentage();
					}
				}

				BigDecimal discountedVariantPrice;
				if (discountPercent != null && discountPercent > 0) {
					BigDecimal multiplier = new BigDecimal(100 - discountPercent).divide(new BigDecimal(100), 2,
							RoundingMode.HALF_UP);
					discountedVariantPrice = baseVariantPrice.multiply(multiplier);
				} else {
					discountedVariantPrice = baseVariantPrice;
				}
				// --- KẾT THÚC TÍNH GIÁ ĐƠN VỊ ĐÃ GIẢM ---

				// --- 7b. TÍNH VÀ SET SUBTOTAL HOÀN CHỈNH ---
				OrderDetail detail = new OrderDetail();
				detail.setOrder(savedOrder);
				detail.setVariant(cartItem.getVariant());
				detail.setQuantity(cartItem.getQuantity());
				detail.setUnitPrice(discountedVariantPrice);

				BigDecimal itemPricePerUnit = discountedVariantPrice;

				// Tính toán giá topping để cộng vào đơn vị
				if (cartItem.getSelectedToppings() != null) {
					for (var topping : cartItem.getSelectedToppings()) {
						itemPricePerUnit = itemPricePerUnit.add(topping.getAdditionalPrice());
					}
				}

				// Tính Final Subtotal = (Giá đơn vị + Topping) * Số lượng
				BigDecimal finalSubtotal = itemPricePerUnit.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

				// *** THAY ĐỔI LỚN: SET SUBTOTAL TRƯỚC KHI LƯU LẦN 1 ***
				detail.setSubtotal(finalSubtotal);

				// --- 7c. LƯU OrderDetail (Chỉ 1 lần để lấy ID) ---
				OrderDetail savedDetail = orderDetailRepository.saveAndFlush(detail);

				// --- 7d. TẠO OrderDetailTopping (Sau khi OrderDetail có ID) ---
				if (cartItem.getSelectedToppings() != null) {
					for (var topping : cartItem.getSelectedToppings()) {
						OrderDetailTopping odt = new OrderDetailTopping();
						odt.setOrderDetail(savedDetail);
						odt.setTopping(topping);
						odt.setUnitPrice(topping.getAdditionalPrice());

						// Thiết lập khóa phức hợp thủ công
						OrderDetailToppingId id = new OrderDetailToppingId();
						id.setOrderDetailID(savedDetail.getOrderDetailID());
						id.setToppingID(topping.getToppingID());
						odt.setId(id);

						orderDetailToppingList.add(odt);
					}
				}

				orderDetailList.add(savedDetail);
			}

			// 7e. LƯU TẤT CẢ TOPPING
			if (!orderDetailToppingList.isEmpty()) {
				orderDetailToppingRepository.saveAll(orderDetailToppingList);
			}

			// === LƯU ORDERHISTORY (PENDING -> CONFIRMED) ===
			OrderHistory history = new OrderHistory();
			history.setOrder(savedOrder);
			history.setOldStatus("Pending");
			history.setNewStatus("Confirmed");
			history.setChangedByUser(user);
			history.setTimestamp(LocalDateTime.now());
			history.setNotes("Đơn hàng được tạo và xác nhận tự động");
			orderHistoryRepository.save(history);

			// === TẠO OrderShippingHistory NẾU CÓ SHIPPER ===
			if (assignedShipper != null) {
				shipperOrderService.createInitialShippingHistory(savedOrder.getOrderID(), assignedShipper.getId(),
						"Đơn hàng được gán tự động cho shipper");
			}

			// === 8. Xóa cart items (Giữ nguyên) ===
			for (CartItem item : itemsToOrder) {
				item.getSelectedToppings().clear();
				cart.removeItem(item);
				cartItemRepository.delete(item);
			}
			cartItemRepository.flush();
			cartRepository.save(cart);

			// === 10. Thanh toán (Giữ nguyên) ===
			if ("Cash".equalsIgnoreCase(paymentMethod)) {
				savedOrder.setOrderStatus("Pending");
				savedOrder.setPaymentStatus("Unpaid");
				orderRepository.save(savedOrder);
				redirectAttributes.addFlashAttribute("orderId", savedOrder.getOrderID());
				return "redirect:/order-success";
			}

			// Trong phương thức placeOrder(), phần VNPay
			if ("VNPay".equalsIgnoreCase(paymentMethod)) {
				String paymentUrl = vnPayService.createPaymentUrl(savedOrder, request);
				return new RedirectView(paymentUrl);
			}
			// 10c. Thanh toán VIETQR (MỚI)
			if ("VietQR".equalsIgnoreCase(paymentMethod)) {

				savedOrder.setOrderStatus("Pending");
				savedOrder.setPaymentStatus("Unpaid");
				orderRepository.save(savedOrder);

				// 1. Chuẩn bị dữ liệu
				String orderCode = String.valueOf(savedOrder.getOrderID());
				String amount = savedOrder.getGrandTotal().setScale(0, RoundingMode.HALF_UP).toPlainString();
				String description = VIETQR_ADD_INFO + orderCode;

				String encodedDescription = java.net.URLEncoder.encode(description, "UTF-8");

				// 2. Tạo URL VietQR API
				// URL có dạng:
				// {BASE_URL}{BANK_ID}-{ACCOUNT_NO}/{AMOUNT}/{DESCRIPTION}?template=compact
				String vietQrUrl = String.format(
						// Cú pháp đúng:
						// {BASE}{BANK_ID}-{ACCOUNT_NO}-{TEMPLATE}.jpg?amount=...&addInfo=...&accountName=...
						"%s%s-%s-%s.jpg?amount=%s&addInfo=%s&accountName=%s", VIETQR_API_URL, // 1.
																								// https://img.vietqr.io/image/
						VIETQR_BANK_ID, // 2. Mã ngân hàng
						VIETQR_ACCOUNT_NO, // 3. Số tài khoản
						VIETQR_TEMPLATE, // 4. Mã template (compact)
						amount, // 5. Số tiền
						URLEncoder.encode(description, StandardCharsets.UTF_8), // 6. Nội dung (encoded)
						URLEncoder.encode(VIETQR_ACCOUNT_NAME, StandardCharsets.UTF_8) // 7. Tên tài khoản (encoded)
				);

				log.info("VietQR URL = {}", vietQrUrl);

				// 3. Chuyển hướng đến trang xác nhận QR
				redirectAttributes.addFlashAttribute("orderId", savedOrder.getOrderID());
				redirectAttributes.addFlashAttribute("qrUrl", vietQrUrl);
				redirectAttributes.addFlashAttribute("amount", savedOrder.getGrandTotal());
				redirectAttributes.addFlashAttribute("description", description);

				return "redirect:/vietqr-confirmation";
			}

			redirectAttributes.addFlashAttribute("errorMessage", "Phương thức thanh toán không hợp lệ.");
			return "redirect:/checkout";

		} catch (ResponseStatusException | UsernameNotFoundException e) {
			if (session != null)
				session.removeAttribute("selectedCheckoutItemIds");
			return "redirect:/login";
		} catch (EntityNotFoundException | AccessDeniedException | IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đặt hàng: " + e.getMessage());
			return "redirect:/checkout";
		} catch (Exception e) {
			log.error("Đã xảy ra lỗi hệ thống khi đặt hàng.", e);
			redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi hệ thống khi đặt hàng.");
			return "redirect:/checkout";
		}
	}

	// Trang thành công (ĐÃ SỬA)
	@GetMapping("/order-success")
	public String orderSuccessPage(Model model, @ModelAttribute("orderId") Integer orderId, HttpSession session) {

		// ✅ LẤY THÔNG TIN SHOP
		Integer selectedShopId = getSelectedShopId(session);

		model.addAttribute("orderId", orderId);
		model.addAttribute("cartItemCount", getCurrentCartItemCount());
		model.addAttribute("categories", categoryService.findAll());

		// ✅ THÊM DỮ LIỆU SHOP
		model.addAttribute("shops", storeService.findAllActiveShops());
		model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

		return "shop/order-success";
	}

	// Trang thất bại (ĐÃ SỬA)
	@GetMapping("/order-failed")
	public String orderFailedPage(Model model, @RequestParam(required = false) String reason, HttpSession session) {

		// ✅ LẤY THÔNG TIN SHOP
		Integer selectedShopId = getSelectedShopId(session);

		model.addAttribute("reason", reason);
		model.addAttribute("cartItemCount", getCurrentCartItemCount());
		model.addAttribute("categories", categoryService.findAll());

		// ✅ THÊM DỮ LIỆU SHOP
		model.addAttribute("shops", storeService.findAllActiveShops());
		model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));

		return "shop/order-failed";
	}

	/**
	 * HIỂN THỊ TRANG XÁC NHẬN THANH TOÁN VIETQR (MỚI)
	 */
	@GetMapping("/vietqr-confirmation")
	public String vietQrConfirmationPage(Model model, @ModelAttribute("orderId") Integer orderId,
			@ModelAttribute("qrUrl") String qrUrl, @ModelAttribute("amount") BigDecimal amount,
			@ModelAttribute("description") String description) {

		if (orderId == null || qrUrl == null) {
			return "redirect:/order-failed?reason=qr_missing_data";
		}

		model.addAttribute("orderId", orderId);
		model.addAttribute("qrUrl", qrUrl);
		model.addAttribute("amount", amount);
		model.addAttribute("description", description);
		model.addAttribute("cartItemCount", getCurrentCartItemCount());
		model.addAttribute("categories", categoryService.findAll());

		return "shop/vietqr-confirmation"; // Cần tạo view này
	}

}