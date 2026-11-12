package com.bakery.account.controller.web;

import com.bakery.account.dto.ProfileUpdateRequest;
import com.bakery.account.dto.ChangePasswordRequest;
import com.bakery.account.dto.AccountAddressRequest;
import com.bakery.account.service.AccountAddressService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequiredArgsConstructor
public class ProfileController {

  private final AccountService accountService;
  private final AccountAddressService accountAddressService;

  @GetMapping("/profile")
  public String profile(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(name = "edit", defaultValue = "false") boolean edit,
      Model model) {
    var profile = accountService.getById(userDetails.getAccountId());
    model.addAttribute("profile", profile);
    ProfileUpdateRequest request = new ProfileUpdateRequest();
    request.setFullName(profile.getFullName());
    request.setPhone(profile.getPhone());
    request.setDateOfBirth(profile.getDateOfBirth());
    model.addAttribute("profileUpdateRequest", request);
    model.addAttribute("profileEditMode", edit);
    return "account/profile";
  }

  @GetMapping("/profile/address")
  public String address(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(name = "edit", required = false) Long editId,
      @RequestParam(name = "add", defaultValue = "false") boolean add,
      Model model) {
    var profile = accountService.getById(userDetails.getAccountId());
    model.addAttribute("profile", profile);
    var addresses = accountAddressService.findByAccountId(userDetails.getAccountId());
    model.addAttribute("addresses", addresses);
    model.addAttribute("editingAddressId", editId);
    model.addAttribute("showAddressForm", add || editId != null);
    AccountAddressRequest request = new AccountAddressRequest();
    request.setReceiverName(profile.getFullName());
    request.setReceiverPhone(profile.getPhone());
    if (editId != null) {
      addresses.stream()
          .filter(address -> address.getId().equals(editId)).findFirst().ifPresent(address -> {
            request.setLabel(address.getLabel());
            request.setReceiverName(address.getReceiverName());
            request.setReceiverPhone(address.getReceiverPhone());
            request.setAddress(address.getAddress());
            request.setDeliveryNote(address.getDeliveryNote());
            request.setDefaultAddress(address.isDefaultAddress());
          });
    }
    model.addAttribute("addressUpdateRequest", request);
    return "account/address";
  }

  @PostMapping("/profile/address")
  public String addAddress(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @ModelAttribute("addressUpdateRequest") AccountAddressRequest request,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("profile", accountService.getById(userDetails.getAccountId()));
      model.addAttribute("addresses", accountAddressService.findByAccountId(userDetails.getAccountId()));
      model.addAttribute("addressUpdateRequest", request);
      model.addAttribute("showAddressForm", true);
      return "account/address";
    }
    accountAddressService.save(userDetails.getAccountId(), null, request);
    return "redirect:/profile/address?updated";
  }

  @PostMapping("/profile/address/{addressId}/update")
  public String updateAddress(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long addressId,
      @Valid @ModelAttribute("addressUpdateRequest") AccountAddressRequest request,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("profile", accountService.getById(userDetails.getAccountId()));
      model.addAttribute("addresses", accountAddressService.findByAccountId(userDetails.getAccountId()));
      model.addAttribute("addressUpdateRequest", request);
      model.addAttribute("editingAddressId", addressId);
      model.addAttribute("showAddressForm", true);
      return "account/address";
    }
    accountAddressService.save(userDetails.getAccountId(), addressId, request);
    return "redirect:/profile/address?updated";
  }

  @PostMapping("/profile/address/{addressId}/default")
  public String setDefaultAddress(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long addressId) {
    accountAddressService.setDefault(userDetails.getAccountId(), addressId);
    return "redirect:/profile/address?updated";
  }

  @PostMapping("/profile/address/{addressId}/delete")
  public String deleteAddress(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long addressId) {
    accountAddressService.delete(userDetails.getAccountId(), addressId);
    return "redirect:/profile/address?updated";
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
      model.addAttribute("profileEditMode", true);
      return "account/profile";
    }

    accountService.updateProfile(userDetails.getAccountId(), request);
    return "redirect:/profile?updated";
  }

  @GetMapping("/profile/security")
  public String security(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      Model model) {
    model.addAttribute("profile", accountService.getById(userDetails.getAccountId()));
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
      model.addAttribute("profile", accountService.getById(userDetails.getAccountId()));
      model.addAttribute("changePasswordRequest", request);
      return "account/security";
    }

    try {
      accountService.changePassword(userDetails.getAccountId(), request);
      return "redirect:/profile/security?updated";
    } catch (InvalidOperationException exception) {
      model.addAttribute("profile", accountService.getById(userDetails.getAccountId()));
      model.addAttribute("changePasswordRequest", request);
      model.addAttribute("passwordError", exception.getMessage());
      return "account/security";
    }
  }
}
