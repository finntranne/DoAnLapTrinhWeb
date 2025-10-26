//package com.alotra.security;
//
//import java.util.Collection;
//import java.util.stream.Collectors;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import com.alotra.entity.user.Role;
//import com.alotra.entity.user.User;
//
//public class MyUserDetails implements UserDetails {
//
//    private static final long serialVersionUID = 1L;
//    private final User user;
//    private final Integer shopId; // 🔹 thêm thuộc tính shopId
//
//    // Constructor mới nhận thêm shopId
//    public MyUserDetails(User user, Integer shopId) {
//        this.user = user;
//        this.shopId = shopId;
//    }
//
//    // Constructor cũ (nếu không có shopId)
//    public MyUserDetails(User user) {
//        this(user, null);
//    }
//
//    // ✅ Getter cho shopId
//    public Integer getShopId() {
//        return shopId;
//    }
//
//    public User getUser() {
//        return user;
//    }
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//<<<<<<< HEAD:src/main/java/com/alotra/service/user/MyUserService.java
//        Set<Role> roles = user.getRoles();
//        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
//        for (Role role : roles) {
//            authorities.add(new SimpleGrantedAuthority(role.getRoleName()));
//        }
//        return authorities;
//=======
//        return user.getRoles().stream()
//                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
//                .collect(Collectors.toList());
//>>>>>>> lam:src/main/java/com/alotra/security/MyUserDetails.java
//    }
//
//    @Override
//    public String getPassword() {
//        return user.getPassword();
//    }
//
//    @Override
//    public String getUsername() {
//        return user.getEmail(); // Dùng email làm username
//    }
//
//    @Override
//    public boolean isAccountNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return true;
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return this.user.getStatus() == 1;
//    }
//}

package com.alotra.security; // Giữ package này

import java.util.Collection;
import java.util.stream.Collectors; // Import cho Stream API
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
// Bỏ import Role nếu không dùng trong loop
import com.alotra.entity.user.User;

public class MyUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;
    private final User user;
    private final Integer shopId; // Giữ lại thuộc tính shopId

    // Constructor nhận user và shopId
    public MyUserDetails(User user, Integer shopId) {
        this.user = user;
        this.shopId = shopId;
    }

    // Constructor cũ (nếu không có shopId)
    public MyUserDetails(User user) {
        this(user, null);
    }

    // Getter cho shopId
    public Integer getShopId() {
        return shopId;
    }

    // Getter cho user
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Sử dụng Stream API từ nhánh lam (ngắn gọn hơn)
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        // Lấy password hash từ User entity
        // Cần đảm bảo User entity có method getPassword() trả về password hash
        // Nếu tên field là passwordHash thì cần getUser().getPasswordHash()
        return user.getPassword(); // Giả sử tên field là passwordHash
    }

    @Override
    public String getUsername() {
        // Dùng email làm username như cả hai nhánh
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Giữ nguyên
    }

    @Override
    public boolean isAccountNonLocked() {
        // Có thể thêm logic kiểm tra user.status == 2 (Suspended) nếu cần
        return user.getStatus() != 2; // Ví dụ: khóa nếu bị đình chỉ
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Giữ nguyên
    }

    @Override
    public boolean isEnabled() {
        // Chỉ enable nếu status là 1 (Active)
        return this.user.getStatus() == 1;
    }
}