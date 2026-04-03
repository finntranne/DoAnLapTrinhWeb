package com.alotra.service.vendor;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.dto.shop.ShopEmployeeDTO;
import com.alotra.entity.user.Role;
import com.alotra.entity.user.User;
import com.alotra.repository.user.RoleRepository;
import com.alotra.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorStaffService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Page<ShopEmployeeDTO> getShopEmployees(Integer shopId, String status, String search, Pageable pageable) {
        List<User> employees = userRepository.findAll().stream()
                .filter(this::isStaffUser)
                .filter(user -> status == null || status.isBlank() || matchesStatus(user, status))
                .filter(user -> search == null || search.isBlank() || matchesSearch(user, search))
                .sorted(Comparator.comparing(User::getId))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), employees.size());
        List<ShopEmployeeDTO> content = start >= employees.size()
                ? List.of()
                : employees.subList(start, end).stream().map(this::toDto).toList();

        return new PageImpl<>(content, pageable, employees.size());
    }

    @Transactional(readOnly = true)
    public User searchUserForEmployee(String searchTerm) {
        User user = searchTerm.contains("@")
                ? userRepository.findByEmail(searchTerm)
                        .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"))
                : userRepository.findByPhoneNumber(searchTerm)
                        .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));

        if (isStaffUser(user)) {
            throw new RuntimeException("Nguoi dung nay da duoc gan vai tro nhan vien");
        }

        return user;
    }

    public void addEmployee(Integer shopId, Integer userId, Integer roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));
        Role role = loadEmployeeRole(roleId);

        user.getRoles().removeIf(existing -> "CUSTOMER".equalsIgnoreCase(existing.getRoleName()));
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public ShopEmployeeDTO getEmployeeDetail(Integer shopId, Integer employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nhan vien"));
        if (!isStaffUser(user)) {
            throw new RuntimeException("Nguoi dung nay khong con la nhan vien");
        }
        return toDto(user);
    }

    public void updateEmployee(Integer shopId, Integer employeeId, Integer newRoleId, String newStatus) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nhan vien"));

        if (newRoleId != null) {
            Role newRole = loadEmployeeRole(newRoleId);
            user.getRoles().removeIf(role -> isEmployeeRole(role.getRoleName()));
            user.getRoles().add(newRole);
        }

        if (newStatus != null && !newStatus.isBlank()) {
            user.setStatus((byte) ("Active".equalsIgnoreCase(newStatus) ? 1 : 0));
        }

        userRepository.save(user);
    }

    public void deactivateEmployee(Integer shopId, Integer employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nhan vien"));

        user.getRoles().removeIf(role -> isEmployeeRole(role.getRoleName()));
        roleRepository.findByRoleName("CUSTOMER").ifPresent(customerRole -> user.getRoles().add(customerRole));
        user.setStatus((byte) 0);
        userRepository.save(user);
    }

    private ShopEmployeeDTO toDto(User user) {
        ShopEmployeeDTO dto = new ShopEmployeeDTO();
        dto.setEmployeeId(user.getId());
        dto.setUserId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAvatarURL(user.getAvatarURL());
        dto.setStatus(user.getStatus() != null && user.getStatus() == 1 ? "Active" : "Inactive");

        user.getRoles().stream()
                .filter(role -> isEmployeeRole(role.getRoleName()))
                .findFirst()
                .ifPresent(role -> {
                    dto.setRoleId(role.getId());
                    dto.setRoleName(role.getRoleName());
                });

        return dto;
    }

    private Role loadEmployeeRole(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay vai tro"));
        if (!isEmployeeRole(role.getRoleName())) {
            throw new RuntimeException("Vai tro khong hop le");
        }
        return role;
    }

    private boolean isStaffUser(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());
        return roleNames.stream().anyMatch(this::isEmployeeRole);
    }

    private boolean isEmployeeRole(String roleName) {
        return "SHIPPER".equalsIgnoreCase(roleName) || "STAFF".equalsIgnoreCase(roleName);
    }

    private boolean matchesStatus(User user, String status) {
        boolean active = user.getStatus() != null && user.getStatus() == 1;
        return ("Active".equalsIgnoreCase(status) && active)
                || ("Inactive".equalsIgnoreCase(status) && !active);
    }

    private boolean matchesSearch(User user, String search) {
        String normalized = search.toLowerCase();
        return (user.getFullName() != null && user.getFullName().toLowerCase().contains(normalized))
                || (user.getEmail() != null && user.getEmail().toLowerCase().contains(normalized))
                || (user.getPhoneNumber() != null && user.getPhoneNumber().toLowerCase().contains(normalized));
    }
}
