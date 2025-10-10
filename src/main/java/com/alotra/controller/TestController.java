package com.alotra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/test")
    public String showTestPage() {
        // Spring Boot sẽ tìm và render file "test-view.html"
        return "test-view"; 
    }
}
