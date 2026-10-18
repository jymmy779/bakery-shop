package com.bakery.account.controller.web;

import com.bakery.account.dto.ForgotPasswordRequest;
import com.bakery.account.dto.RegisterRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

  @GetMapping("/login")
  public String login() {
    return "auth/login";
  }

  @GetMapping("/register")
  public String register(Model model) {
    model.addAttribute("registerRequest", new RegisterRequest());
    return "auth/register";
  }

  @GetMapping("/forgot-password")
  public String forgotPassword(Model model) {
    model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
    return "auth/forgot-password";
  }
}
