package com.bakery.account.controller.admin;

import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.dto.AccountStatusUpdateRequest;
import com.bakery.account.dto.ChangePasswordRequest;
import com.bakery.account.dto.CmsProfileUpdateRequest;
import com.bakery.account.dto.ProfileUpdateRequest;
import com.bakery.account.dto.StaffCreateRequest;
import com.bakery.account.dto.StaffUpdateRequest;
import com.bakery.account.entity.AccountStatus;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
      @ModelAttribute("profileUpdateRequest") @Valid CmsProfileUpdateRequest request,
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
  public String accounts(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) AccountStatus status,
      @RequestParam(defaultValue = "newest") String order,
      Pageable pageable,
      Model model) {
    String selectedOrder = normalizeOrder(order);
    model.addAttribute("accounts", accountService.findAll(search, status, selectedOrder, pageable));
    addAccountFilters(model, search, status, selectedOrder);
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
  public String staff(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) AccountStatus status,
      @RequestParam(defaultValue = "newest") String order,
      Pageable pageable,
      Model model) {
    String selectedOrder = normalizeOrder(order);
    model.addAttribute("staffPage", accountService.findStaff(search, status, selectedOrder, pageable));
    addAccountFilters(model, search, status, selectedOrder);
    return "cms/staff/list";
  }

  private void addAccountFilters(Model model, String search, AccountStatus status, String order) {
    model.addAttribute("search", search == null ? "" : search.trim());
    model.addAttribute("selectedStatus", status == null ? "" : status.name());
    model.addAttribute("selectedOrder", order);
    model.addAttribute("hasFilters", (search != null && !search.isBlank()) || status != null);
  }

  private String normalizeOrder(String order) {
    if ("oldest".equals(order) || "name_asc".equals(order) || "name_desc".equals(order)) {
      return order;
    }
    return "newest";
  }

  @GetMapping("/staff/add")
  public String newStaff(Model model) {
    populateStaffForm(model, false, null, new StaffCreateRequest());
    return "cms/staff/form";
  }

  @GetMapping("/staff/{id}/edit")
  public String editStaff(@PathVariable Long id, Model model) {
    var staff = accountService.getStaffById(id);
    StaffUpdateRequest request = new StaffUpdateRequest();
    request.setFullName(staff.getFullName());
    request.setEmail(staff.getEmail());
    request.setPhone(staff.getPhone());
    model.addAttribute("staff", staff);
    populateStaffForm(model, true, id, request);
    return "cms/staff/form";
  }

  @PostMapping("/staff")
  public String createStaff(
      @ModelAttribute("staffRequest") @Valid StaffCreateRequest request,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      populateStaffForm(model, false, null, request);
      return "cms/staff/form";
    }

    try {
      accountService.createStaff(request);
      redirectAttributes.addFlashAttribute("successMessage", "Đã thêm nhân viên.");
    } catch (RuntimeException exception) {
      populateStaffForm(model, false, null, request);
      model.addAttribute("errorMessage", exception.getMessage());
      return "cms/staff/form";
    }
    return "redirect:/cms/staff";
  }

  @PostMapping("/staff/{id}/update")
  public String updateStaff(
      @PathVariable Long id,
      @ModelAttribute("staffRequest") @Valid StaffUpdateRequest request,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("staff", accountService.getStaffById(id));
      populateStaffForm(model, true, id, request);
      return "cms/staff/form";
    }

    try {
      accountService.updateStaff(id, request);
      redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin nhân viên.");
    } catch (RuntimeException exception) {
      model.addAttribute("staff", accountService.getStaffById(id));
      populateStaffForm(model, true, id, request);
      model.addAttribute("errorMessage", exception.getMessage());
      return "cms/staff/form";
    }
    return "redirect:/cms/staff";
  }

  @PostMapping("/staff/{id}/status")
  public String updateStaffStatus(
      @PathVariable Long id,
      @Valid AccountStatusUpdateRequest request,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái tài khoản không hợp lệ.");
      return "redirect:/cms/staff";
    }

    try {
      accountService.updateStaffStatus(id, request);
      redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái nhân viên.");
    } catch (RuntimeException exception) {
      redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/staff";
  }

  private CmsProfileUpdateRequest toUpdateRequest(AccountProfileResponse profile) {
    CmsProfileUpdateRequest request = new CmsProfileUpdateRequest();
    request.setFullName(profile.getFullName());
    request.setPhone(profile.getPhone());
    return request;
  }

  private void populateStaffForm(Model model, boolean editing, Long staffId, Object request) {
    model.addAttribute("isEdit", editing);
    model.addAttribute("staffId", staffId);
    model.addAttribute("staffRequest", request);
  }
}
