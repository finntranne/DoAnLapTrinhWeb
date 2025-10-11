package com.alotra.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;



@Controller
public class LoginController {
  
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }
    
    @PostMapping("/login_success_handler")
    public String loginSuccessHandler(){
        System.out.println("logging user login success...");
        return "home/index";
    }

    @PostMapping("/login_failure_handler")
    public String loginFailureHandler(){
        System.out.println("login failure handler...");
        return "login";
    }



}
