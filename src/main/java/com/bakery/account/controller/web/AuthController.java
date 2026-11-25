package com.bakery.account.controller.web;

import com.bakery.account.dto.ForgotPasswordRequest;
import com.bakery.account.dto.ResetPasswordRequest;
import com.bakery.account.dto.RegisterRequest;
import com.bakery.account.dto.VerifyOtpRequest;
import com.bakery.account.service.AccountService;
import com.bakery.account.service.PasswordResetService;
import com.bakery.common.exception.InvalidOperationException;
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
  private final PasswordResetService passwordResetService;

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
    model.addAttribute("step", "email");
    return "auth/forgot-password";
  }

  @PostMapping("/forgot-password/send-otp")
  public String sendPasswordResetOtp(
      @Valid ForgotPasswordRequest request,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("forgotPasswordRequest", request);
      model.addAttribute("step", "email");
      return "auth/forgot-password";
    }

    try {
      passwordResetService.sendOtp(request);
      VerifyOtpRequest verifyRequest = new VerifyOtpRequest();
      verifyRequest.setEmail(request.getEmail().trim());
      model.addAttribute("verifyOtpRequest", verifyRequest);
      model.addAttribute("step", "otp");
      model.addAttribute("notice", "Nếu email thuộc tài khoản đang hoạt động, mã OTP sẽ được gửi. Hãy kiểm tra hộp thư và thư rác.");
    } catch (IllegalStateException exception) {
      model.addAttribute("forgotPasswordRequest", request);
      model.addAttribute("step", "email");
      model.addAttribute("errorMessage", "Chưa gửi được email. Vui lòng thử lại sau hoặc liên hệ quản trị viên.");
    }
    return "auth/forgot-password";
  }

  @PostMapping("/forgot-password/verify-otp")
  public String verifyPasswordResetOtp(
      @Valid VerifyOtpRequest request,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("verifyOtpRequest", request);
      model.addAttribute("step", "otp");
      return "auth/forgot-password";
    }

    try {
      ResetPasswordRequest resetRequest = new ResetPasswordRequest();
      resetRequest.setResetToken(passwordResetService.verifyOtp(request));
      model.addAttribute("resetPasswordRequest", resetRequest);
      model.addAttribute("step", "reset");
    } catch (InvalidOperationException exception) {
      model.addAttribute("verifyOtpRequest", request);
      model.addAttribute("step", "otp");
      model.addAttribute("errorMessage", exception.getMessage());
    }
    return "auth/forgot-password";
  }

  @PostMapping("/forgot-password/reset")
  public String resetPassword(
      @Valid ResetPasswordRequest request,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("resetPasswordRequest", request);
      model.addAttribute("step", "reset");
      return "auth/forgot-password";
    }

    try {
      passwordResetService.resetPassword(request);
      return "redirect:/login?passwordReset";
    } catch (InvalidOperationException exception) {
      ResetPasswordRequest retryRequest = new ResetPasswordRequest();
      retryRequest.setResetToken(request.getResetToken());
      model.addAttribute("resetPasswordRequest", retryRequest);
      model.addAttribute("step", "reset");
      model.addAttribute("errorMessage", exception.getMessage());
      return "auth/forgot-password";
    }
  }
}
