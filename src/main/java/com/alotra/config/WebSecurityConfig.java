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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

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

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable()) // Disable CSRF cho REST API
				.authorizeHttpRequests(authorize -> authorize
						// Public endpoints - không cần authentication
						.requestMatchers("/", "/login", "/api/auth/**", "/css/**", "/js/**", "/images/**", "/assets/**",
								"/vendor/**", "/api/debug/**")
						.permitAll()

						// Role-based access
						.requestMatchers("/admin/**").hasAuthority("ADMIN").requestMatchers("/vendor/**")
						.hasAuthority("VENDOR").requestMatchers("/shipper/**").hasAuthority("SHIPPER")
						.requestMatchers("/customer/**").hasAuthority("CUSTOMER")

						// Tất cả request khác cần authentication
						.anyRequest().authenticated())
				// Không dùng form login mặc định nữa, vì đã có REST API
				.formLogin(login -> login.disable())

				// Session management
				.sessionManagement(
						session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).maximumSessions(1) // Chỉ
																														// cho
																														// phép
																														// 1
																														// session/user
				)

//				.logout(logout -> logout.logoutUrl("/api/auth/logout") // URL endpoint logout
//						.logoutSuccessHandler((request, response, authentication) -> {
//							response.setStatus(200);
//							response.setContentType("application/json;charset=UTF-8");
//							response.getWriter().write("{\"message\":\"Đăng xuất thành công\"}");
//						}).permitAll().invalidateHttpSession(true).deleteCookies("JSESSIONID")
//						.clearAuthentication(true))

				.exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, authException) -> {
					// Nếu chưa đăng nhập, trả về 401
					if (request.getRequestURI().startsWith("/api/")) {
						response.setStatus(401);
						response.setContentType("application/json");
						response.getWriter()
								.write("{\"errorCode\":\"UNAUTHORIZED\",\"message\":\"Vui lòng đăng nhập\"}");
					} else {
						response.sendRedirect("/login");
					}
				}).accessDeniedHandler((request, response, accessDeniedException) -> {
					// Nếu không có quyền, trả về 403
					if (request.getRequestURI().startsWith("/api/")) {
						response.setStatus(403);
						response.setContentType("application/json");
						response.getWriter()
								.write("{\"errorCode\":\"FORBIDDEN\",\"message\":\"Bạn không có quyền truy cập\"}");
					} else {
						response.sendRedirect("/auth/403");
					}
				})).build();
	}
}