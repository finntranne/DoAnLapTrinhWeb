package com.alotra.service.vendor;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.shop.ShopEmployeeDTO;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.shop.ShopEmployee;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.repository.shop.ShopEmployeeRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.user.RoleRepository;
import com.alotra.repository.user.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorStaffService {

	private final ShopRepository shopRepository;
	private final UserRepository userRepository;
	private final ShopEmployeeRepository shopEmployeeRepository;
	private final RoleRepository roleRepository;

	@PersistenceContext // Inject EntityManager for JPQL
	private EntityManager entityManager;

	// ==================== STAFF MANAGEMENT ====================
	@Transactional(readOnly = true)
	public Page<ShopEmployeeDTO> getShopEmployees(Integer shopId, String status, String search, Pageable pageable) {

		Page<ShopEmployee> employees = shopEmployeeRepository.findShopEmployeesFiltered(shopId, status, search,
				pageable);

		return employees.map(employee -> {
			ShopEmployeeDTO dto = new ShopEmployeeDTO();
			dto.setEmployeeId(employee.getEmployeeId());
			dto.setUserId(employee.getUser().getId());
			dto.setFullName(employee.getUser().getFullName());
			dto.setEmail(employee.getUser().getEmail());
			dto.setPhoneNumber(employee.getUser().getPhoneNumber());
			dto.setAvatarURL(employee.getUser().getAvatarURL());
			dto.setStatus(employee.getStatus());
			dto.setAssignedAt(employee.getAssignedAt());
			dto.setUpdatedAt(employee.getUpdatedAt());

			// Lấy role name (STAFF hoặc SHIPPER)
			employee.getUser().getRoles().stream()
					.filter(role -> "STAFF".equals(role.getRoleName()) || "SHIPPER".equals(role.getRoleName()))
					.findFirst().ifPresent(role -> {
						dto.setRoleId(role.getId());
						dto.setRoleName(role.getRoleName());
					});

			return dto;
		});
	}

	@Transactional(readOnly = true)
	public User searchUserForEmployee(String searchTerm) {
		// Tìm user theo email hoặc phone
		Optional<User> userOpt;

		if (searchTerm.contains("@")) {
			userOpt = userRepository.findByEmail(searchTerm);
		} else {
			userOpt = userRepository.findByPhoneNumber(searchTerm);
		}

		if (!userOpt.isPresent()) {
			throw new RuntimeException("Không tìm thấy người dùng với thông tin: " + searchTerm);
		}

		User user = userOpt.get();

		// Kiểm tra user phải có status = 1 (Active)
		if (user.getStatus() != 1) {
			throw new RuntimeException("Tài khoản này chưa được kích hoạt hoặc đã bị khóa");
		}

		// Kiểm tra user phải có role CUSTOMER
		boolean isCustomer = user.getRoles().stream().anyMatch(role -> "CUSTOMER".equals(role.getRoleName()));

		if (!isCustomer) {
			throw new RuntimeException("Người dùng này không phải là khách hàng hoặc đã có vai trò khác");
		}

		return user;
	}

	@Transactional
	public void addEmployee(Integer shopId, Integer userId, Integer roleId) {

		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));

		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		// Kiểm tra user đã là employee của shop này chưa
		if (shopEmployeeRepository.existsByShop_ShopIdAndUser_Id(shopId, userId)) {
			throw new RuntimeException("Người dùng này đã là nhân viên của cửa hàng");
		}

		// Kiểm tra roleId hợp lệ (4: SHIPPER, 5: STAFF)
		if (roleId != 4 && roleId != 5) {
			throw new RuntimeException("Vai trò không hợp lệ");
		}

		Role newRole = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));

		// Xóa role CUSTOMER và thêm role mới
		user.getRoles().removeIf(role -> "CUSTOMER".equals(role.getRoleName()));
		user.getRoles().add(newRole);
		userRepository.save(user);

		// Tạo ShopEmployee record
		ShopEmployee employee = new ShopEmployee();
		employee.setShop(shop);
		employee.setUser(user);
		employee.setStatus("Active");

		shopEmployeeRepository.save(employee);

		log.info("Added employee: User {} to Shop {} with role {}", userId, shopId, newRole.getRoleName());
	}

	@Transactional(readOnly = true)
	public ShopEmployeeDTO getEmployeeDetail(Integer shopId, Integer employeeId) {

		ShopEmployee employee = shopEmployeeRepository.findById(employeeId)
				.orElseThrow(() -> new RuntimeException("Employee not found"));

		if (!employee.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Employee does not belong to this shop");
		}

		ShopEmployeeDTO dto = new ShopEmployeeDTO();
		dto.setEmployeeId(employee.getEmployeeId());
		dto.setUserId(employee.getUser().getId());
		dto.setFullName(employee.getUser().getFullName());
		dto.setEmail(employee.getUser().getEmail());
		dto.setPhoneNumber(employee.getUser().getPhoneNumber());
		dto.setAvatarURL(employee.getUser().getAvatarURL());
		dto.setStatus(employee.getStatus());
		dto.setAssignedAt(employee.getAssignedAt());
		dto.setUpdatedAt(employee.getUpdatedAt());

		// Lấy role hiện tại
		employee.getUser().getRoles().stream()
				.filter(role -> "STAFF".equals(role.getRoleName()) || "SHIPPER".equals(role.getRoleName())).findFirst()
				.ifPresent(role -> {
					dto.setRoleId(role.getId());
					dto.setRoleName(role.getRoleName());
				});

		return dto;
	}

	@Transactional
	public void updateEmployee(Integer shopId, Integer employeeId, Integer newRoleId, String newStatus) {

		ShopEmployee employee = shopEmployeeRepository.findById(employeeId)
				.orElseThrow(() -> new RuntimeException("Employee not found"));

		if (!employee.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Employee does not belong to this shop");
		}

		User user = employee.getUser();

		// Cập nhật role nếu thay đổi
		if (newRoleId != null && (newRoleId == 4 || newRoleId == 5)) {
			Role currentRole = user.getRoles().stream()
					.filter(role -> "STAFF".equals(role.getRoleName()) || "SHIPPER".equals(role.getRoleName()))
					.findFirst().orElse(null);

			if (currentRole == null || !currentRole.getId().equals(newRoleId)) {
				// Xóa role cũ
				user.getRoles()
						.removeIf(role -> "STAFF".equals(role.getRoleName()) || "SHIPPER".equals(role.getRoleName()));

				// Thêm role mới
				Role newRole = roleRepository.findById(newRoleId)
						.orElseThrow(() -> new RuntimeException("Role not found"));
				user.getRoles().add(newRole);
				userRepository.save(user);

				log.info("Updated employee role: User {} to {}", user.getId(), newRole.getRoleName());
			}
		}

		// Cập nhật status
		if (newStatus != null && ("Active".equals(newStatus) || "Inactive".equals(newStatus))) {
			employee.setStatus(newStatus);
			shopEmployeeRepository.save(employee);

			log.info("Updated employee status: Employee {} to {}", employeeId, newStatus);
		}
	}

	@Transactional
	public void deactivateEmployee(Integer shopId, Integer employeeId) {

		ShopEmployee employee = shopEmployeeRepository.findById(employeeId)
				.orElseThrow(() -> new RuntimeException("Employee not found"));

		if (!employee.getShop().getShopId().equals(shopId)) {
			throw new RuntimeException("Unauthorized: Employee does not belong to this shop");
		}

		// Chuyển status thành Inactive
		employee.setStatus("Inactive");
		shopEmployeeRepository.save(employee);

		// Optional: Có thể chuyển user về role CUSTOMER
		User user = employee.getUser();
		user.getRoles().removeIf(role -> "STAFF".equals(role.getRoleName()) || "SHIPPER".equals(role.getRoleName()));

		Role customerRole = roleRepository.findById(2) // CUSTOMER role ID = 2
				.orElseThrow(() -> new RuntimeException("Customer role not found"));
		user.getRoles().add(customerRole);
		userRepository.save(user);

		log.info("Deactivated employee: Employee {}, User {} reverted to CUSTOMER", employeeId, user.getId());
	}

}
