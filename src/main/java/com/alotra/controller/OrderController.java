package com.alotra.controller; // Giữ package này

import com.alotra.config.VNPayConfig;
// Import các entity
import com.alotra.entity.cart.Cart;
import com.alotra.entity.cart.CartItem;
import com.alotra.entity.location.Address;
import com.alotra.entity.order.Order;
import com.alotra.entity.order.OrderDetail;
import com.alotra.entity.order.OrderDetailTopping;
import com.alotra.entity.order.OrderDetailToppingId;
import com.alotra.entity.order.OrderHistory;
import com.alotra.entity.shop.ShopEmployee;
import com.alotra.entity.user.User;
import com.alotra.repository.cart.CartItemRepository;
import com.alotra.repository.cart.CartRepository;
import com.alotra.repository.order.OrderDetailRepository;
import com.alotra.repository.order.OrderDetailToppingRepository;
import com.alotra.repository.order.OrderHistoryRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.repository.shop.ShopEmployeeRepository;
import com.alotra.repository.location.AddressRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.order.ShipperOrderService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.user.UserService;
import com.alotra.util.VNPayUtil;
// *** THÊM IMPORT CHO LOGIC GIẢM GIÁ ***
import com.alotra.service.product.ProductService;
import com.alotra.model.ProductSaleDTO;
import java.math.RoundingMode;
// **************************************

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class OrderController {

	@Autowired
	private OrderRepository orderRepository;
//    @Autowired private VNPayService vnPayService;
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
	private OrderHistoryRepository orderHistoryRepository;
	@Autowired 
	private ShipperOrderService shipperOrderService;
	@Autowired
	private UserService userService;
	@Autowired
	private ShopEmployeeRepository shopEmployeeRepository;
	@Autowired
	private com.alotra.config.VNPayConfig vnPayConfig;

	// *** THÊM: Inject ProductService để lấy giảm giá ***
	@Autowired
	private ProductService productService;

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
		return (selectedShopId == null) ? 0 : selectedShopId; // Mặc định là 0 (Xem tất cả)
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
			System.err.println("Lỗi chọn item checkout: " + e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi chọn sản phẩm.");
			return "redirect:/cart";
		}
	}

	/**
	 * *** HIỂN THỊ TRANG CHECKOUT (Giữ nguyên logic sau sửa chữa) ***
	 */
	@GetMapping("/checkout")
	public String showCheckoutPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {

		try {
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

			// Tính toán tổng tiền
			BigDecimal subtotal = cartService.calculateSubtotal(new HashSet<>(itemsToCheckout));

			BigDecimal shippingFee = new BigDecimal("20000");
			BigDecimal discount = BigDecimal.ZERO;
			BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);

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

			return "shop/checkout";

		} catch (ResponseStatusException | UsernameNotFoundException e) {
			return "redirect:/login";
		} catch (Exception e) {
			System.err.println("Lỗi khi tải trang checkout: " + e.getMessage());
			e.printStackTrace();
			session.removeAttribute("selectedCheckoutItemIds");
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tải trang thanh toán.");
			return "redirect:/cart";
		}
	}

	/**
	 * *** SỬA LỚN: XỬ LÝ ĐẶT HÀNG (ĐẢM BẢO SUBTOTAL VÀ ID TRƯỚC KHI LƯU) ***
	 */
	@PostMapping("/place-order")
	@Transactional
	public Object placeOrder(@RequestParam(required = false) Integer addressId,
			@RequestParam(required = false) String notes, @RequestParam(required = false) String paymentMethod,
			HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {

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
			@SuppressWarnings("unchecked")
			List<Integer> selectedItemIds = (List<Integer>) session.getAttribute("selectedCheckoutItemIds");

			if (selectedItemIds == null || selectedItemIds.isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn hoặc chưa chọn sản phẩm.");
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
			order.setDiscountAmount(discount);
			order.setGrandTotal(grandTotal);
			order.setNotes(notes);

			// *** TỰ ĐỘNG GÁN SHIPPER ***
			User assignedShipper = assignShipperAutomatically(order.getShop().getShopId());
			if (assignedShipper != null) {
				order.setShipper(assignedShipper);
			}

			savedOrder = orderRepository.saveAndFlush(order);

			// === 7. SỬA LỚN: Xử lý OrderDetail và Topping ===
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

			// === 9. Xóa session (Giữ nguyên) ===
			session.removeAttribute("selectedCheckoutItemIds");

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
				savedOrder.setOrderStatus("Pending");
				savedOrder.setPaymentStatus("Processing");
				orderRepository.save(savedOrder);

				String vnp_TxnRef = String.valueOf(savedOrder.getOrderID());
				String vnp_OrderInfo = "Thanh toan don hang #" + savedOrder.getOrderID();

				// ---- vnp_Amount: chính xác, không mất số lẻ ----
				String vnp_Amount = savedOrder.getGrandTotal().multiply(new BigDecimal("100"))
						.setScale(0, RoundingMode.HALF_UP).toPlainString();

				Map<String, String> vnp_Params = new HashMap<>();
				vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
				vnp_Params.put("vnp_Command", VNPayConfig.vnp_Command);
				vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
				vnp_Params.put("vnp_Amount", vnp_Amount);
				vnp_Params.put("vnp_CurrCode", "VND");
				vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
				vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
				vnp_Params.put("vnp_OrderType", "other");
				vnp_Params.put("vnp_Locale", "vn");
				vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
				vnp_Params.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

				// ⭐ THÊM DÒNG NÀY - BẮT BUỘC
				vnp_Params.put("vnp_CreateDate", VNPayUtil.getCreateDate());

				// ---- Tạo hash ----
				String queryForHash = VNPayUtil.getQueryString(vnp_Params);
				String vnp_SecureHash = VNPayUtil.hmacSHA512(VNPayConfig.vnp_HashSecret, queryForHash);

				// ---- URL cuối cùng ----
				String paymentUrl = VNPayConfig.vnp_Url + "?" + queryForHash + "&vnp_SecureHash=" + vnp_SecureHash;

				System.out.println("=== VNPAY PAYMENT REQUEST ===");
				vnp_Params.forEach((k, v) -> System.out.println(k + "=" + v));
				System.out.println("Query for hash: " + queryForHash);
				System.out.println("SecureHash: " + vnp_SecureHash);
				System.out.println("Payment URL: " + paymentUrl);
				System.out.println("==============================");

				return new RedirectView(paymentUrl);
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
			redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi hệ thống khi đặt hàng.");
			return "redirect:/checkout";
		}
	}

	// Trang thành công (Giữ nguyên)
	@GetMapping("/order-success")
	public String orderSuccessPage(Model model, @ModelAttribute("orderId") Integer orderId) {
		model.addAttribute("orderId", orderId);
		model.addAttribute("cartItemCount", getCurrentCartItemCount());
		model.addAttribute("categories", categoryService.findAll());
		return "shop/order-success";
	}

	// Trang thất bại (Giữ nguyên)
	@GetMapping("/order-failed")
	public String orderFailedPage(Model model, @RequestParam(required = false) String reason) {
		model.addAttribute("reason", reason);
		model.addAttribute("cartItemCount", getCurrentCartItemCount());
		model.addAttribute("categories", categoryService.findAll());
		return "shop/order-failed";
	}

	/**
	 * Xử lý kết quả trả về từ VNPay
	 */
	@GetMapping("/vnpay-return")
	public String vnpayReturn(@RequestParam Map<String, String> queryParams, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {

		try {
			String vnp_SecureHash = queryParams.get("vnp_SecureHash");
			queryParams.remove("vnp_SecureHash");

			String signValue = VNPayUtil.hmacSHA512(VNPayConfig.vnp_HashSecret, VNPayUtil.getQueryString(queryParams));
			boolean isValidSignature = signValue.equals(vnp_SecureHash);

			if (!isValidSignature) {
				redirectAttributes.addFlashAttribute("errorMessage", "Chữ ký không hợp lệ.");
				return "redirect:/order-failed?reason=invalid_signature";
			}

			String vnp_ResponseCode = queryParams.get("vnp_ResponseCode");
			String vnp_TxnRef = queryParams.get("vnp_TxnRef");

			if ("00".equals(vnp_ResponseCode)) {
				// Thanh toán thành công
				Optional<Order> orderOpt = orderRepository.findById(Integer.parseInt(vnp_TxnRef));
				if (orderOpt.isPresent()) {
					Order order = orderOpt.get();
					order.setPaymentStatus("Paid");
					order.setOrderStatus("Confirmed");
					order.setPaidAt(LocalDateTime.now());
					orderRepository.save(order);

					redirectAttributes.addFlashAttribute("orderId", order.getOrderID());
					return "redirect:/order-success";
				}
			}

			// Thanh toán thất bại
			System.err.println("VNPay payment failed with code: " + vnp_ResponseCode);
			redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán thất bại. Mã lỗi: " + vnp_ResponseCode);
			return "redirect:/order-failed?reason=" + vnp_ResponseCode;

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý thanh toán.");
			return "redirect:/order-failed?reason=system_error";
		}
	}

}