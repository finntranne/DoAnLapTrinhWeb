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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.alotra.security.jwt.AuthEntryPointJwt;
import com.alotra.security.jwt.JwtAuthenticationFilter;
import com.alotra.service.user.impl.UserServiceImpl;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;
	
	@Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

//	@Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//      return http
//            .csrf(csrf -> csrf.disable())
//            .authorizeHttpRequests(authorize -> authorize
//                    // --- CHO PHÉP TRUY CẬP CÔNG KHAI ---
//                    .requestMatchers(
//                        "/", "/home/**", "/test",      
//                        "/auth/**", "/login", "/register", 
//                        "/products/**",             
//                        "/api/auth/**", // Chỉ cho phép api/auth
//                        "/api/debug/**",
//                        "/css/**", "/js/**", "/images/**", "/assets/**"
//                        // ĐÃ XÓA /vendor/** khỏi đây
//                    ).permitAll()
//                    
//                    // --- PHÂN QUYỀN CHO CÁC VAI TRÒ CỤ THỂ ---
//                    .requestMatchers("/admin/**").hasAuthority("ADMIN")
//                    .requestMatchers("/vendor/**").hasAuthority("VENDOR") // Sẽ hoạt động
//                    .requestMatchers("/shipper/**").hasAuthority("SHIPPER")
//                    .requestMatchers("/user/**", "/user/profile/**", "/user/addresses/**", "/user/orders/**").hasAuthority("CUSTOMER")
//                
//                // --- CÁC TRANG CẦN ĐĂNG NHẬP (Bất kỳ quyền nào) ---
//                .requestMatchers("/checkout/**", "/place-order", "/cart/**", "/cart/buy-now").authenticated()
//                
//                // --- API CẦN ĐĂNG NHẬP ---
//                // (Nếu bạn có các API không phải /api/auth)
//                .requestMatchers("/api/**").authenticated() // Yêu cầu tất cả API khác phải đăng nhập
//
//                // --- TẤT CẢ CÁC REQUEST CÒN LẠI ---
//                .anyRequest().authenticated()
//            )
//			.formLogin(login -> login.disable())
//            .logout(logout -> logout
//                .logoutUrl("/logout")
//                .logoutSuccessUrl("/login?logout")
//                .permitAll()
//            )
//            .sessionManagement(
//            		session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).maximumSessions(1)
//            )
//            .exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, authException) -> {
//    					// Nếu chưa đăng nhập, trả về 401
//    					if (request.getRequestURI().startsWith("/api/")) {
//    						response.setStatus(401);
//    						response.setContentType("application/json");
//    						response.getWriter()
//    								.write("{\"errorCode\":\"UNAUTHORIZED\",\"message\":\"Vui lòng đăng nhập\"}");
//    					} else {
//    						response.sendRedirect("/login");
//    					}
//    				}).accessDeniedHandler((request, response, accessDeniedException) -> {
//    					// Nếu không có quyền, trả về 403
//    					if (request.getRequestURI().startsWith("/api/")) {
//    						response.setStatus(403);
//    						response.setContentType("application/json");
//    						response.getWriter()
//    								.write("{\"errorCode\":\"FORBIDDEN\",\"message\":\"Bạn không có quyền truy cập\"}");
//    					} else {
//    						response.sendRedirect("/auth/403");
//    					}
//    				})).build();
//    }
	
	
	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // Disable CSRF cho API REST
                
                // *** THÊM JWT FILTER VÀO CHAIN ***
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                .authorizeHttpRequests(authorize -> authorize
                        // --- CHO PHÉP TRUY CẬP CÔNG KHAI ---
                        .requestMatchers(
                            "/", "/home/**", "/test",      
                            "/auth/**", "/login", "/register",
                            "/reset-password",
                            "/products/**",             
                            "/api/auth/**", // API đăng nhập/đăng ký
                            "/api/debug/**",
                            "/css/**", "/js/**", "/images/**", "/assets/**","/ws/**",
                            "/favicon.ico", "/error"
                        ).permitAll()
                        
                        // --- PHÂN QUYỀN CHO CÁC VAI TRÒ CỤ THỂ ---
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/vendor/**").hasAuthority("VENDOR")
                        .requestMatchers("/shipper/**").hasAuthority("SHIPPER")
                        .requestMatchers("/user/**", "/user/profile/**", "/user/addresses/**", "/user/orders/**")
                            .hasAnyAuthority("CUSTOMER", "VENDOR")
                    
                        // --- CÁC TRANG CẦN ĐĂNG NHẬP (Bất kỳ quyền nào) ---
                        .requestMatchers("/checkout/**", "/place-order", "/cart/**", "/cart/buy-now")
                            .authenticated()
                        
                        // --- API CẦN ĐĂNG NHẬP ---
                        .requestMatchers("/api/**").authenticated()

                        // --- TẤT CẢ CÁC REQUEST CÒN LẠI ---
                        .anyRequest().authenticated()
                )
                
                // *** DISABLE FORM LOGIN (Vì dùng JWT cho API) ***
                .formLogin(login -> login.disable())
                
//                .logout(logout -> logout
//                    .logoutUrl("/logout")
//                    .logoutSuccessUrl("/login?logout")
//                    .permitAll()
//                )
                .logout(logout -> logout.disable())
                
                // *** QUAN TRỌNG: Đổi sang STATELESS cho JWT ***
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // *** XỬ LÝ EXCEPTION ***
                .exceptionHandling(handling -> handling
                    .authenticationEntryPoint(unauthorizedHandler) // Sử dụng AuthEntryPointJwt
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        // Nếu không có quyền
                        if (request.getRequestURI().startsWith("/api/")) {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                "{\"errorCode\":\"FORBIDDEN\",\"message\":\"Bạn không có quyền truy cập\"}"
                            );
                        } else {
                            response.sendRedirect("/auth/403");
                        }
                    })
                )
                .build();
    }
	
}
