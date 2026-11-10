package com.bakery.account.controller.web;

import com.bakery.account.dto.ProfileUpdateRequest;
import com.bakery.account.dto.ChangePasswordRequest;
import com.bakery.account.security.CustomUserDetails;
import com.bakery.account.service.AccountService;
import com.bakery.common.exception.InvalidOperationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class ProfileController {

  private final AccountService accountService;

  @GetMapping("/profile")
  public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    var profile = accountService.getById(userDetails.getAccountId());
    model.addAttribute("profile", profile);
    ProfileUpdateRequest request = new ProfileUpdateRequest();
    request.setFullName(profile.getFullName());
    request.setPhone(profile.getPhone());
    request.setDateOfBirth(profile.getDateOfBirth());
    model.addAttribute("profileUpdateRequest", request);
    return "account/profile";
  }

  @PostMapping("/profile")
  public String updateProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid ProfileUpdateRequest request,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("profile", accountService.getById(userDetails.getAccountId()));
      model.addAttribute("profileUpdateRequest", request);
      return "account/profile";
    }

    accountService.updateProfile(userDetails.getAccountId(), request);
    return "redirect:/profile?updated";
  }

  @GetMapping("/profile/security")
  public String security(Model model) {
    model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
    return "account/security";
  }

  @PostMapping("/profile/security")
  public String changePassword(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid ChangePasswordRequest request,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("changePasswordRequest", request);
      return "account/security";
    }

    try {
      accountService.changePassword(userDetails.getAccountId(), request);
      return "redirect:/profile/security?updated";
    } catch (InvalidOperationException exception) {
      model.addAttribute("changePasswordRequest", request);
      model.addAttribute("passwordError", exception.getMessage());
      return "account/security";
    }
  }
}
