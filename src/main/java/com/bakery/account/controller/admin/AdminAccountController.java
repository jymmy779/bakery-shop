package com.bakery.account.controller.admin;

import com.bakery.account.dto.StaffCreateRequest;
import com.bakery.account.dto.StaffUpdateRequest;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class AdminAccountController {

  private final AccountService accountService;

  @GetMapping({"/cms/accounts", "/admin/customers"})
  public String customers(
      @RequestParam(defaultValue = "") String keyword,
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    Page<?> customers = accountService.findByRoleAndKeyword(
        Role.ROLE_CUSTOMER, keyword, pageRequest(page));
    model.addAttribute("customers", customers);
    model.addAttribute("keyword", keyword);
    if (!customers.hasContent() && !keyword.isBlank()) {
      model.addAttribute("errorMessage", "Không tìm thấy tài khoản khách hàng phù hợp");
    }
    return "admin/customers";
  }

  @PostMapping({"/cms/accounts/toggle-status/{id}", "/admin/customers/toggle-status/{id}"})
  public String toggleCustomerStatus(
      @PathVariable Long id, RedirectAttributes redirect) {
    try {
      if (accountService.getById(id).getRole() != Role.ROLE_CUSTOMER) {
        throw new IllegalArgumentException("Tài khoản không phải khách hàng.");
      }
      var account = accountService.toggleAccountStatus(id);
      redirect.addFlashAttribute("successMessage",
          account.getStatus() == AccountStatus.LOCKED
              ? "Khóa tài khoản thành công!" : "Mở khóa tài khoản thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/admin/customers";
  }

  @GetMapping({"/cms/staff", "/admin/staffs"})
  public String staff(
      @RequestParam(defaultValue = "") String keyword,
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    Page<?> staffs = accountService.findByRoleAndKeyword(
        Role.ROLE_STAFF, keyword, pageRequest(page));
    model.addAttribute("staffs", staffs);
    model.addAttribute("keyword", keyword);
    model.addAttribute("createRequest", new StaffCreateRequest());
    if (!staffs.hasContent() && !keyword.isBlank()) {
      model.addAttribute("errorMessage", "Không tìm thấy nhân viên phù hợp");
    }
    return "admin/staffs";
  }

  @PostMapping({"/cms/staff/add", "/admin/staffs/add"})
  public String createStaff(
      @Valid @ModelAttribute("createRequest") StaffCreateRequest request,
      BindingResult result, RedirectAttributes redirect) {
    if (result.hasErrors()) {
        addValidationErrors(result, redirect);
      return "redirect:/admin/staffs";
    }
    try {
      accountService.createStaffAccount(request);
      redirect.addFlashAttribute("successMessage",
          "Tạo tài khoản nhân viên thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/admin/staffs";
  }

  @PostMapping({"/cms/staff/update/{id}", "/admin/staffs/update/{id}"})
  public String updateStaff(
      @PathVariable Long id,
      @Valid @ModelAttribute StaffUpdateRequest request,
      BindingResult result, RedirectAttributes redirect) {
    if (result.hasErrors()) {
        addValidationErrors(result, redirect);
      return "redirect:/admin/staffs";
    }
    try {
      accountService.updateStaff(id, request);
      redirect.addFlashAttribute("successMessage",
          "Cập nhật thông tin nhân viên thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/admin/staffs";
  }

  @PostMapping({"/cms/staff/toggle-status/{id}", "/admin/staffs/toggle-status/{id}"})
  public String toggleStaffStatus(
      @PathVariable Long id, RedirectAttributes redirect) {
    try {
      if (accountService.getById(id).getRole() != Role.ROLE_STAFF) {
        throw new IllegalArgumentException("Tài khoản không phải nhân viên.");
      }
      var account = accountService.toggleAccountStatus(id);
      redirect.addFlashAttribute("successMessage",
          account.getStatus() == AccountStatus.LOCKED
              ? "Khóa tài khoản thành công!" : "Mở khóa tài khoản thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/admin/staffs";
  }

  private PageRequest pageRequest(int page) {
    return PageRequest.of(Math.max(page, 0), 10,
        Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  private void addValidationErrors(BindingResult result, RedirectAttributes redirect) {
    List<String> errors = new ArrayList<>();
    result.getFieldErrors().forEach(error -> errors.add(fieldLabel(error.getField())
        + ": " + error.getDefaultMessage()));
    result.getGlobalErrors().forEach(error -> errors.add(error.getDefaultMessage()));
    redirect.addFlashAttribute("errorMessage", "Vui lòng kiểm tra lại thông tin nhân viên.");
    redirect.addFlashAttribute("validationErrors", errors);
  }

  private String fieldLabel(String field) {
    return switch (field) {
      case "fullName" -> "Họ tên";
      case "email" -> "Email";
      case "phone" -> "Số điện thoại";
      case "password" -> "Mật khẩu";
      default -> field;
    };
  }
}
