package com.bakery.account.controller.admin;

import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.dto.AccountStatusUpdateRequest;
import com.bakery.account.dto.ChangePasswordRequest;
import com.bakery.account.dto.CmsProfileUpdateRequest;
import com.bakery.account.dto.ProfileUpdateRequest;
import com.bakery.account.security.CustomUserDetails;
import com.bakery.account.service.AccountService;
import com.bakery.common.exception.InvalidOperationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cms")
@RequiredArgsConstructor
public class AdminAccountController {

  private final AccountService accountService;

  @GetMapping("/profile")
  public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    AccountProfileResponse profile = accountService.getById(userDetails.getAccountId());
    model.addAttribute("profile", profile);
    model.addAttribute("profileUpdateRequest", toUpdateRequest(profile));
    return "cms/account/profile";
  }

  @PostMapping("/profile")
  public String updateProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid CmsProfileUpdateRequest request,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("profile", accountService.getById(userDetails.getAccountId()));
      model.addAttribute("profileUpdateRequest", request);
      return "cms/account/profile";
    }

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
    updateRequest.setFullName(request.getFullName());
    updateRequest.setPhone(request.getPhone());
    AccountProfileResponse currentProfile = accountService.getById(userDetails.getAccountId());
    updateRequest.setAddress(currentProfile.getAddress());
    updateRequest.setDateOfBirth(currentProfile.getDateOfBirth());
    accountService.updateProfile(userDetails.getAccountId(), updateRequest);
    redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hồ sơ tài khoản.");
    return "redirect:/cms/profile";
  }

  @GetMapping("/profile/security")
  public String security(Model model) {
    model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
    return "cms/account/security";
  }

  @PostMapping("/profile/security")
  public String changePassword(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid ChangePasswordRequest request,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("changePasswordRequest", request);
      return "cms/account/security";
    }

    try {
      accountService.changePassword(userDetails.getAccountId(), request);
      redirectAttributes.addFlashAttribute("successMessage", "Mật khẩu đã được thay đổi thành công.");
      return "redirect:/cms/profile/security";
    } catch (InvalidOperationException exception) {
      model.addAttribute("changePasswordRequest", request);
      model.addAttribute("errorMessage", exception.getMessage());
      return "cms/account/security";
    }
  }

  @GetMapping("/accounts")
  public String accounts(Pageable pageable, Model model) {
    model.addAttribute("accounts", accountService.findAll(pageable));
    return "cms/account/list";
  }

  @PostMapping("/accounts/{id}/status")
  public String updateStatus(
      @PathVariable Long id,
      @Valid AccountStatusUpdateRequest request,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái tài khoản không hợp lệ.");
      return "redirect:/cms/accounts";
    }

    try {
      accountService.updateStatus(id, request);
      redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái tài khoản.");
    } catch (RuntimeException exception) {
      redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/accounts";
  }

  @GetMapping("/staff")
  public String staff() {
    return "cms/staff/list";
  }

  private CmsProfileUpdateRequest toUpdateRequest(AccountProfileResponse profile) {
    CmsProfileUpdateRequest request = new CmsProfileUpdateRequest();
    request.setFullName(profile.getFullName());
    request.setPhone(profile.getPhone());
    return request;
  }
}
