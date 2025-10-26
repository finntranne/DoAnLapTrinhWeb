package com.alotra.service.user.impl; // Hoặc package .impl tùy cấu trúc của bạn

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.alotra.entity.user.User;             // Import User
import com.alotra.repository.user.UserRepository;
//import com.alotra.service.user.MyUserService;
import com.alotra.service.user.UserService;

import java.util.Optional;                      // Import Optional

@Service
// Sửa đổi: implements cả hai interface
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

//    // --- Hàm của UserDetailsService (Giữ nguyên) ---
//    @Override
//    public UserDetails loadUserByUsername(String usernameOrEmail) // Đổi tên tham số cho rõ ràng
//            throws UsernameNotFoundException {
//
//        // Giả sử UserRepository có hàm tìm bằng username hoặc email
//        // Ví dụ: findByUsernameOrEmail(usernameOrEmail)
//        // Hoặc bạn có thể thử tìm bằng email trước, nếu không thấy thì tìm bằng username
//        User user = userRepository.findByEmail(usernameOrEmail) // Thử tìm bằng email
//                .orElseGet(() -> userRepository.getUserByUsername(usernameOrEmail)); // Nếu không có email thì tìm bằng username (hàm cũ của bạn)
//
//        if (user == null) {
//            throw new UsernameNotFoundException("Could not find user with username or email: " + usernameOrEmail);
//        }
//
//        // Giả sử MyUserService là implementation của UserDetails
//        return new MyUserService(user);
//    }

    // === THÊM HÀM CỦA UserService ===
    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

	@Override
	public User save(User user) {
	    return userRepository.save(user);
	}

	@Override
	public Optional<User> findByPhoneNumber(String phoneNumber) {
		return userRepository.findByPhoneNumber(phoneNumber);
	}


}