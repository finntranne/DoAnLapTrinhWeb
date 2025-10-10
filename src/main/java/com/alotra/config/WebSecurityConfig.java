package com.alotra.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.alotra.service.user.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Giữ nguyên, rất tốt cho việc phân quyền ở cấp độ phương thức
public class WebSecurityConfig {

    // Sử dụng @Autowired UserDetailsService mà Spring đã quản lý
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    // Bean này không cần thiết nếu bạn đã có @Autowired ở trên,
    // nhưng nếu bạn muốn định nghĩa rõ ràng, hãy làm như sau:
    @Bean
    public UserDetailsService userDetailsService() {
        // QUAN TRỌNG: Trả về instance đã được inject, không "new" ở đây
        return customUserDetailsService;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // Sử dụng UserDetailsService đã được Spring quản lý
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // KHÔNG CẦN PHƯƠNG THỨC NÀY NỮA (configure(AuthenticationManagerBuilder auth))
    // Vì DaoAuthenticationProvider bean đã làm nhiệm vụ tương tự.

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        // Rút gọn lại, đây là cách chuẩn trong Spring Boot 3
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tạm thời tắt CSRF để test, nên bật trong môi trường production
            .authorizeHttpRequests(authorize -> authorize
                // --- CHO PHÉP TRUY CẬP CÔNG KHAI ---
                .requestMatchers(
                		"/", "/home/**", "/test",      // <-- SỬA Ở ĐÂY: từ "/home" thành "/home/**"
                        "/auth/**", "/login", "/register",
                        "/products/**",
                        "/css/**", "/js/**", "/images/**"
                ).permitAll()
                
                // --- PHÂN QUYỀN DỰA TRÊN VAI TRÒ (ROLE) ---
                // Ví dụ: Các trang quản trị yêu cầu quyền ADMIN
                .requestMatchers("/admin/**", "/new", "/edit/**", "/delete/**").hasAuthority("ADMIN")
                
                // --- API (Nếu có) ---
                // Mở API cho tất cả mọi người (hoặc cấu hình lại tùy theo nhu cầu)
                .requestMatchers("/api/**").permitAll()

                // --- TẤT CẢ CÁC REQUEST CÒN LẠI ---
                // Yêu cầu phải được xác thực
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login") // URL xử lý đăng nhập
                .defaultSuccessUrl("/", true) // Chuyển hướng về trang chủ sau khi đăng nhập thành công
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout") // Chuyển hướng về trang login với thông báo
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/403") // Trang lỗi khi không có quyền
            );

        return http.build();
    }
}