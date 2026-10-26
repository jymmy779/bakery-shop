package com.bakery.account.controller.web;

import com.bakery.account.dto.ForgotPasswordRequest;
import com.bakery.account.dto.RegisterRequest;
import com.bakery.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;

@Controller
@RequiredArgsConstructor
public class AuthController {

  private final AccountService accountService;

  @GetMapping("/login")
  public String login() {
    return "auth/login";
  }

  @GetMapping("/register")
  public String register(Model model) {
    model.addAttribute("registerRequest", new RegisterRequest());
    return "auth/register";
  }

  @PostMapping("/register")
  public String registerCustomer(
      @Valid RegisterRequest request,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("registerRequest", request);
      return "auth/register";
    }

    try {
      accountService.registerCustomer(request);
      return "redirect:/login?registered";
    } catch (RuntimeException exception) {
      bindingResult.reject("register.failed", exception.getMessage());
      model.addAttribute("registerRequest", request);
      return "auth/register";
    }
  }

  @GetMapping("/forgot-password")
  public String forgotPassword(Model model) {
    model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
    return "auth/forgot-password";
  }
}
