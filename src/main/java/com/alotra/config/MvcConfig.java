package com.alotra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		// Auth templates
		registry.addViewController("/login").setViewName("auth/login");
		registry.addViewController("/auth/403").setViewName("auth/403");
		registry.addViewController("/auth/forgot-password").setViewName("auth/forgot-password");

		// Dashboard
		registry.addViewController("/dashboard").setViewName("dashboard");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new HandlerInterceptor() {
			@Override
			public void postHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
					Object handler, ModelAndView modelAndView) {
				if (modelAndView != null) {
					modelAndView.addObject("currentUri", request.getRequestURI());
				}
			}
		});
	}
}