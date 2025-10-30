package com.alotra.controller.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class OAuth2CallbackController {

	@GetMapping("/oauth2/success")
    public String oauth2Success(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "oauth2-success"; // File: templates/oauth2-success.html
    }
}