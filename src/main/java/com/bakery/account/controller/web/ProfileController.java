package com.bakery.account.controller.web;

import com.bakery.account.dto.ProfileUpdateRequest;
import com.bakery.account.security.CustomUserDetails;
import com.bakery.account.service.AccountService;
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
    model.addAttribute("profile", accountService.getById(userDetails.getAccountId()));
    model.addAttribute("profileUpdateRequest", new ProfileUpdateRequest());
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
}
